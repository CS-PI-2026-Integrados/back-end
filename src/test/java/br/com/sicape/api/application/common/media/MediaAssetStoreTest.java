package br.com.sicape.api.application.common.media;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import br.com.sicape.api.application.common.storage.FileStorage;
import br.com.sicape.api.domain.entity.MediaAsset;
import br.com.sicape.api.domain.enums.MediaAssetKind;
import br.com.sicape.api.domain.repository.MediaAssetRepository;

class MediaAssetStoreTest {
    private final FileStorage storage = mock(FileStorage.class);
    private final MediaAssetRepository assets = mock(MediaAssetRepository.class);
    private final MediaAssetStore store = new MediaAssetStore(storage, assets);

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        when(assets.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void storesPhotosAndReceiptsUnderTheirOwnDirectories() {
        MediaAsset photo = store.save(new byte[]{1}, "image/jpeg", MediaAssetKind.PHOTO);
        MediaAsset receipt = store.save(new byte[]{2}, "application/pdf", MediaAssetKind.RECEIPT);

        assertThat(photo.getStorageKey()).isEqualTo("photo/" + photo.getUuid());
        assertThat(receipt.getStorageKey()).isEqualTo("receipt/" + receipt.getUuid());
        verify(storage).save(photo.getStorageKey(), new byte[]{1}, "image/jpeg");
        verify(storage).save(receipt.getStorageKey(), new byte[]{2}, "application/pdf");
    }

    @Test
    void deletesContentUsingItsStorageKey() {
        MediaAsset asset = photo();

        store.deleteContent(asset);

        verify(storage).delete(asset.getStorageKey());
    }

    @Test
    void removesCreatedContentAfterRollback() {
        MediaAsset created = save();

        verify(storage, never()).delete(created.getStorageKey());
        synchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storage).delete(created.getStorageKey());
    }

    @Test
    void preservesCreatedContentAfterCommit() {
        MediaAsset created = save();

        synchronization().afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        verify(storage, never()).delete(created.getStorageKey());
    }

    @Test
    void preservesCreatedContentWhenTransactionStatusIsUnknown() {
        MediaAsset created = save();

        synchronization().afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);

        verify(storage, never()).delete(created.getStorageKey());
    }

    @Test
    void removesReplacedContentOnlyAfterCommit() {
        MediaAsset replaced = photo();

        store.removeAfterCommit(replaced);

        verify(assets).delete(replaced);
        verify(assets).flush();
        verify(storage, never()).delete(replaced.getStorageKey());

        synchronization().afterCommit();

        verify(storage).delete(replaced.getStorageKey());
    }

    @Test
    void preservesReplacedContentAfterRollback() {
        MediaAsset replaced = photo();

        store.removeAfterCommit(replaced);
        synchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storage, never()).delete(replaced.getStorageKey());
    }

    @Test
    void logsRollbackCleanupFailureWithoutThrowing() {
        MediaAsset created = save();
        doThrow(new IllegalStateException("cleanup failure"))
            .when(storage).delete(created.getStorageKey());

        assertThatCode(() -> synchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK))
            .doesNotThrowAnyException();
    }

    @Test
    void logsAfterCommitCleanupFailureWithoutThrowing() {
        MediaAsset replaced = photo();
        doThrow(new IllegalStateException("cleanup failure"))
            .when(storage).delete(replaced.getStorageKey());
        store.removeAfterCommit(replaced);

        assertThatCode(() -> synchronization().afterCommit()).doesNotThrowAnyException();
    }

    @Test
    void preservesDatabaseFailureWhenImmediateCleanupAlsoFails() {
        var databaseFailure = new IllegalStateException("database failure");
        var cleanupFailure = new IllegalStateException("cleanup failure");
        when(assets.saveAndFlush(any())).thenThrow(databaseFailure);
        doThrow(cleanupFailure).when(storage).delete(any());

        assertThatThrownBy(this::save)
            .isSameAs(databaseFailure)
            .hasSuppressedException(cleanupFailure);
    }

    @Test
    void rejectsSaveWithoutActiveTransactionBeforeWritingContent() {
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);

        assertThatThrownBy(this::save)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("transação sincronizada");
        verifyNoInteractions(storage, assets);
    }

    private MediaAsset save() {
        return store.save(new byte[]{1}, "image/jpeg", MediaAssetKind.PHOTO);
    }

    private TransactionSynchronization synchronization() {
        return TransactionSynchronizationManager.getSynchronizations().getFirst();
    }

    private static MediaAsset photo() {
        UUID uuid = UUID.randomUUID();
        return new MediaAsset(uuid, "photo/" + uuid, "image/jpeg", 3, MediaAssetKind.PHOTO);
    }
}
