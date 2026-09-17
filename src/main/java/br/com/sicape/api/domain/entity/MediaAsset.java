package br.com.sicape.api.domain.entity;

import java.util.UUID;

import br.com.sicape.api.domain.enums.MediaAssetKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = @Index(name = "idx_media_asset_storage_key", columnList = "storage_key", unique = true))
public class MediaAsset extends BaseEntity {
    @Column(nullable = false, unique = true, updatable = false, length = 255)
    private String storageKey;

    @Column(nullable = false, updatable = false, length = 100)
    private String contentType;

    @Column(nullable = false, updatable = false)
    private long size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private MediaAssetKind kind;

    public MediaAsset(UUID uuid, String storageKey, String contentType, long size, MediaAssetKind kind) {
        if (uuid == null) {
            throw new IllegalArgumentException("O identificador da mídia é obrigatório");
        }
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("A referência de armazenamento é obrigatória");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("O tipo da mídia é obrigatório");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("O tamanho da mídia deve ser positivo");
        }
        if (kind == null) {
            throw new IllegalArgumentException("A finalidade da mídia é obrigatória");
        }

        setUuid(uuid);
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.size = size;
        this.kind = kind;
    }
}
