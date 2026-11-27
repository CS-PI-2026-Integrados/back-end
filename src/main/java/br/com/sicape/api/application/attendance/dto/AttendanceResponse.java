package br.com.sicape.api.application.attendance.dto;

import java.time.Instant;
import java.util.UUID;

import br.com.sicape.api.application.common.dto.response.AddressResponse;
import br.com.sicape.api.domain.entity.Attendance;
import br.com.sicape.api.domain.enums.EmploymentStatus;

public record AttendanceResponse(
    UUID id,
    UUID convictedId,
    UUID processId,
    AddressResponse address,
    String phone,
    EmploymentStatus employmentStatus,
    UUID userId,
    String userName,
    Instant createdAt,
    Instant updatedAt,
    String processNumber,
    boolean receiptIssued,
    boolean photoAvailable,
    boolean noProcessDeclared
) {
    public AttendanceResponse(UUID id, UUID convictedId, UUID processId, AddressResponse address, String phone, EmploymentStatus employmentStatus, UUID userId, String userName, Instant createdAt, Instant updatedAt, String processNumber, boolean receiptIssued, boolean photoAvailable) {
        this(id, convictedId, processId, address, phone, employmentStatus, userId, userName, createdAt, updatedAt, processNumber, receiptIssued, photoAvailable, false);
    }

    public static AttendanceResponse from(Attendance attendance) {
        var process = attendance.getProcess();
        return new AttendanceResponse(
            attendance.getUuid(),
            attendance.getConvicted().getUuid(),
            process == null ? null : process.getUuid(),
            AddressResponse.from(attendance.getAddress()),
            attendance.getPhone() == null ? null : attendance.getPhone().value(),
            attendance.getEmploymentStatus(),
            attendance.getUser().getUuid(),
            attendance.getUser().getName(),
            attendance.getCreatedAt(),
            attendance.getUpdatedAt(),
            process == null ? null : process.getNumber(),
            attendance.getReceipt() != null,
            attendance.getPhoto() != null,
            attendance.isNoProcessDeclared()
        );
    }
}
