package br.com.sicape.api.application.attendance.dto;

import java.util.UUID;

import br.com.sicape.api.application.common.dto.request.AddressRequest;
import br.com.sicape.api.domain.enums.EmploymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAttendanceRequest(
    @NotNull(message = "O apenado é obrigatório") UUID convictedId,
    @NotNull(message = "O processo é obrigatório") UUID processId,
    @NotNull(message = "O endereço é obrigatório") @Valid AddressRequest address,
    @NotBlank(message = "O telefone é obrigatório") String phone,
    @NotNull(message = "A situação trabalhista é obrigatória") EmploymentStatus employmentStatus
) {}
