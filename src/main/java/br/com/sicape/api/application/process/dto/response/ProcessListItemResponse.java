package br.com.sicape.api.application.process.dto.response;

import java.util.UUID;
import java.util.List;

import br.com.sicape.api.domain.enums.ProcessStatus;

public record ProcessListItemResponse(
    UUID id,
    String number,
    ProcessStatus status,
    long linkedConvictedCount,
    List<String> linkedConvictedNames
) {}