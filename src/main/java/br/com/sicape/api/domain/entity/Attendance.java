package br.com.sicape.api.domain.entity;

import br.com.sicape.api.domain.enums.EmploymentStatus;
import br.com.sicape.api.domain.enums.MediaAssetKind;
import br.com.sicape.api.domain.valueobject.Address;
import br.com.sicape.api.domain.valueobject.Phone;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = {
    @Index(name = "idx_attendance_district", columnList = "district_id"),
    @Index(name = "idx_attendance_convicted", columnList = "convicted_id"),
    @Index(name = "idx_attendance_process", columnList = "process_id"),
    @Index(name = "idx_attendance_created_at", columnList = "created_at")
})
public class Attendance extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false, updatable = false)
    private Convicted convicted;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false, updatable = false)
    private JudicialProcess process;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false, updatable = false)
    private JudicialDistrict district;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false, updatable = false)
    private User user;

    @Embedded
    private Address address;

    @Embedded
    @AttributeOverride(
        name = "value",
        column = @Column(name = "phone", nullable = false, updatable = false, length = 11)
    )
    private Phone phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private EmploymentStatus employmentStatus;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "photo_asset_id", nullable = false, updatable = false, unique = true)
    private MediaAsset photo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_asset_id", unique = true)
    private MediaAsset receipt;

    public Attendance(
        Convicted convicted,
        JudicialProcess process,
        JudicialDistrict district,
        User user,
        Address address,
        Phone phone,
        EmploymentStatus employmentStatus,
        MediaAsset photo
    ) {
        if (convicted == null) {
            throw new IllegalArgumentException("O condenado é obrigatório");
        }
        if (process == null) {
            throw new IllegalArgumentException("O processo é obrigatório");
        }
        if (district == null) {
            throw new IllegalArgumentException("A comarca é obrigatória");
        }
        if (user == null) {
            throw new IllegalArgumentException("O usuário responsável é obrigatório");
        }
        if (address == null) {
            throw new IllegalArgumentException("O endereço é obrigatório");
        }
        if (phone == null) {
            throw new IllegalArgumentException("O telefone é obrigatório");
        }
        if (employmentStatus == null) {
            throw new IllegalArgumentException("A situação empregatícia é obrigatória");
        }
        if (photo == null || photo.getKind() != MediaAssetKind.PHOTO) {
            throw new IllegalArgumentException("A foto do atendimento é obrigatória");
        }

        this.convicted = convicted;
        this.process = process;
        this.district = district;
        this.user = user;
        this.address = address;
        this.phone = phone;
        this.employmentStatus = employmentStatus;
        this.photo = photo;
    }

    public void completeReceipt(MediaAsset receipt) {
        if (receipt == null || receipt.getKind() != MediaAssetKind.RECEIPT) {
            throw new IllegalArgumentException("O comprovante deve ser uma mídia do tipo recibo");
        }
        this.receipt = receipt;
    }
}
