package br.com.sicape.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Immutable identity and cadastral photo as known when the attendance was recorded. */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceReceiptContext {
    @Column(name = "receipt_context_version", updatable = false)
    private Integer version;

    @Column(name = "receipt_convicted_name", updatable = false)
    private String convictedName;

    @Column(name = "receipt_convicted_cpf", length = 14, updatable = false)
    private String convictedCpf;

    @Column(name = "receipt_process_number", length = 50, updatable = false)
    private String processNumber;

    @Column(name = "receipt_district_name", updatable = false)
    private String districtName;

    @Column(name = "receipt_operator_name", updatable = false)
    private String operatorName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_convicted_photo_asset_id", updatable = false,
        foreignKey = @ForeignKey(name = "fk_attendance_receipt_convicted_photo"))
    private MediaAsset convictedPhoto;

    public AttendanceReceiptContext(Convicted convicted, JudicialProcess process, JudicialDistrict district, User user) {
        version = 1;
        convictedName = convicted.getName();
        convictedCpf = convicted.getCpf() == null ? null : convicted.getCpf().formatted();
        processNumber = process == null ? null : process.getNumber();
        districtName = district.getName();
        operatorName = user.getName();
        convictedPhoto = convicted.getPhoto();
    }
}
