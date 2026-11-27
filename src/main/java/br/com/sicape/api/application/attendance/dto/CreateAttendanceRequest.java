package br.com.sicape.api.application.attendance.dto;

import java.util.UUID;

import br.com.sicape.api.application.common.dto.request.AddressRequest;
import br.com.sicape.api.domain.enums.EmploymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateAttendanceRequest(
    @NotNull(message = "O apenado é obrigatório") UUID convictedId,
    UUID processId,
    @NotNull(message = "O endereço é obrigatório") @Valid AddressRequest address,
    String phone,
    @NotNull(message = "A situação trabalhista é obrigatória") EmploymentStatus employmentStatus,
    boolean noProcessDeclared
) {
    public CreateAttendanceRequest(UUID convictedId, UUID processId, AddressRequest address,
        String phone, EmploymentStatus employmentStatus) {
        this(convictedId, processId, address, phone, employmentStatus, false);
    }
}
