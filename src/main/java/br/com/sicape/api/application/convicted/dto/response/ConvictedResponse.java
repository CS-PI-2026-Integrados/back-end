package br.com.sicape.api.application.convicted.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import br.com.sicape.api.domain.enums.ConvictedStatus;
import br.com.sicape.api.domain.enums.EmploymentStatus;

public record ConvictedResponse(
    UUID id,
    String name,
    String cpf,
    LocalDate birthDate,
    String phone,
    AddressResponse address,
    EmploymentStatus employmentStatus,
    ConvictedStatus status,
    String photoUrl,
    List<ProcessResponse> processes
) {}
