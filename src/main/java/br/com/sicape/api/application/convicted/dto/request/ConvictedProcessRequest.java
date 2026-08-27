package br.com.sicape.api.application.convicted.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ConvictedProcessRequest(
    @NotNull(message = "O ID do processo é obrigatório") UUID id,
    boolean principal
) {}
