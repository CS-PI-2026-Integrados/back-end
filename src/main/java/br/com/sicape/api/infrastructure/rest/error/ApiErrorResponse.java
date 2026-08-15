package br.com.sicape.api.infrastructure.rest.error;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        ApiErrorDebug debug
) {
}