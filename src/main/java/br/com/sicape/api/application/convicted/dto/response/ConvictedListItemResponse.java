package br.com.sicape.api.application.convicted.dto.response;

import java.util.UUID;

import br.com.sicape.api.application.common.dto.response.AddressResponse;
import br.com.sicape.api.domain.enums.EmploymentStatus;

public record ConvictedListItemResponse(
    UUID id,
    String name,
    String cpf,
    String phone,
    AddressResponse address,
    EmploymentStatus employmentStatus,
    String mainProcessNumber,
    long sameProcessConvictedCount
) {}
