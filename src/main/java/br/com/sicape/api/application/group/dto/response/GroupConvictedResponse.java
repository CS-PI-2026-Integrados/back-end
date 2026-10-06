package br.com.sicape.api.application.group.dto.response;

import java.util.UUID;

public record GroupConvictedResponse(
    UUID id,
    String name,
    String document
) {}