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
    Instant createdAt,
    Instant updatedAt
) {
    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(
            attendance.getUuid(),
            attendance.getConvicted().getUuid(),
            attendance.getProcess().getUuid(),
            AddressResponse.from(attendance.getAddress()),
            attendance.getPhone().value(),
            attendance.getEmploymentStatus(),
            attendance.getUser().getUuid(),
            attendance.getCreatedAt(),
            attendance.getUpdatedAt()
        );
    }
}
