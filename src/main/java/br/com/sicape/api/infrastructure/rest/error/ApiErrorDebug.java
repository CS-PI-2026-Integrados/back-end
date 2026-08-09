package br.com.sicape.api.infrastructure.rest.error;

import java.util.List;

public record ApiErrorDebug(
        String exception,
        String message,
        List<String> traceList
) {}