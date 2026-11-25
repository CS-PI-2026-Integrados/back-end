package br.com.sicape.api.application.process.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateProcessRequest(
    @NotBlank(message = "O número do processo é obrigatório") String number
) {}
