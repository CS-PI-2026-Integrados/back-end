package br.com.sicape.api.application.convicted.dto.request;

import java.time.LocalDate;

import br.com.sicape.api.domain.enums.EmploymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

public record CreateConvictedRequest(
    @NotBlank(message = "O nome é obrigatório") String name,
    @NotBlank(message = "O CPF é obrigatório") String cpf,
    @NotNull(message = "A data de nascimento é obrigatória")
    @PastOrPresent(message = "A data de nascimento não pode ser futura") LocalDate birthDate,
    @NotBlank(message = "O telefone é obrigatório") String phone,
    @NotNull(message = "O endereço é obrigatório") @Valid AddressRequest address,
    EmploymentStatus employmentStatus
) {}
