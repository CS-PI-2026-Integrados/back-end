package br.com.sicape.api.infrastructure.rest.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<ApiFieldError> fields,
        ApiErrorDebug debug
) {
}
