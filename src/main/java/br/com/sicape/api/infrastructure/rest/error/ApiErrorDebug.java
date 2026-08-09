package br.com.sicape.api.infrastructure.rest.error;

public record ApiErrorDebug(
        String exception,
        String message
) {}