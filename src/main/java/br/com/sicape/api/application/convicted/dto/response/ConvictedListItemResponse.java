package br.com.sicape.api.application.convicted.dto.response;

import java.util.UUID;

import br.com.sicape.api.domain.enums.EmploymentStatus;

public record ConvictedListItemResponse(
    UUID id,
    String name,
    String cpf,
    String photoUrl,
    String phone,
    AddressResponse address,
    EmploymentStatus employmentStatus,
    String mainProcessNumber,
    long sameProcessConvictedCount
) {}
