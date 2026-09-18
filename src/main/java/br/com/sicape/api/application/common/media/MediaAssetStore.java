package br.com.sicape.api.application.common.media;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import br.com.sicape.api.application.common.storage.FileStorage;
import br.com.sicape.api.domain.entity.MediaAsset;
import br.com.sicape.api.domain.enums.MediaAssetKind;
import br.com.sicape.api.domain.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaAssetStore {
    private final FileStorage storage;
    private final MediaAssetRepository assets;

    @Transactional(propagation = Propagation.MANDATORY)
    public MediaAsset save(byte[] content, String contentType, MediaAssetKind kind) {
        requireTransactionSynchronization();
        UUID uuid = UUID.randomUUID();
        String key = uuid.toString();

        storage.save(key, content, contentType);
        try {
            MediaAsset asset = assets.saveAndFlush(
                new MediaAsset(uuid, key, contentType, content.length, kind)
            );
            registerRollbackCleanup(asset);
            return asset;
        } catch (RuntimeException | Error failure) {
            deleteImmediately(key, uuid, failure);
            throw failure;
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void removeAfterCommit(MediaAsset asset) {
        if (asset == null) {
            return;
        }
        requireTransactionSynchronization();
        assets.delete(asset);
        assets.flush();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteSafely(asset, "after_commit");
            }
        });
    }

    public MediaContent read(MediaAsset asset) {
        return new MediaContent(storage.read(asset.getStorageKey()), asset.getContentType());
    }

    public void deleteContent(MediaAsset asset) {
        storage.delete(asset.getStorageKey());
    }

    private void registerRollbackCleanup(MediaAsset asset) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteSafely(asset, "rollback");
                } else if (status == STATUS_UNKNOWN) {
                    log.error(
                        "Estado transacional desconhecido; conteúdo preservado para reconciliação; assetUuid={}",
                        asset.getUuid()
                    );
                }
            }
        });
    }

    private void requireTransactionSynchronization() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
            || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("A operação de mídia exige uma transação sincronizada");
        }
    }

    private void deleteImmediately(String key, UUID assetUuid, Throwable originalFailure) {
        try {
            storage.delete(key);
        } catch (RuntimeException cleanupFailure) {
            originalFailure.addSuppressed(cleanupFailure);
            log.error(
                "Falha ao compensar conteúdo após erro ao persistir mídia; assetUuid={}",
                assetUuid,
                cleanupFailure
            );
        }
    }

    private void deleteSafely(MediaAsset asset, String phase) {
        try {
            storage.delete(asset.getStorageKey());
        } catch (RuntimeException failure) {
            log.error(
                "Falha ao remover conteúdo de mídia; phase={}, assetUuid={}",
                phase,
                asset.getUuid(),
                failure
            );
        }
    }
}
