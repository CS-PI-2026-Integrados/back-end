package br.com.sicape.api.application.convicted.dto.response;

import java.util.UUID;

import br.com.sicape.api.domain.enums.ProcessStatus;

public record ProcessResponse(
    UUID id,
    String number,
    ProcessStatus status,
    boolean principal
) {}
