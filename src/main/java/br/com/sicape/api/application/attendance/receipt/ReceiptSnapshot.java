package br.com.sicape.api.application.attendance.receipt;

import java.time.Instant;
import java.util.UUID;

import br.com.sicape.api.domain.entity.Attendance;

public record ReceiptSnapshot(
    UUID attendanceId,
    Instant attendedAt,
    String address,
    String phone,
    String employmentStatus,
    String convictedName,
    String convictedCpf,
    String processNumber,
    String districtName,
    String courtName,
    String operatorName,
    String protocol,
    String templateXhtml,
    String logoSha256
) {
    public static ReceiptSnapshot create(
        Attendance attendance,
        String templateXhtml,
        String logoSha256
    ) {
        return new ReceiptSnapshot(
            attendance.getUuid(),
            attendance.getCreatedAt(),
            attendance.getAddress().formatted(),
            attendance.getPhone().formatted(),
            attendance.getEmploymentStatus().displayName(),
            attendance.getConvicted().getName(),
            attendance.getConvicted().getCpf().formatted(),
            attendance.getProcess().getNumber(),
            attendance.getDistrict().getName(),
            "Vara de Execuções Penais", // Mantido hardcode até incluirmos uma entidade de configuração por comarca
            attendance.getUser().getName(),
            attendance.getUuid().toString(),
            templateXhtml,
            logoSha256
        );
    }
}
