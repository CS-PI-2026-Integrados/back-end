package br.com.sicape.api.infrastructure.rest.error;

import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.sicape.api.domain.exception.DomainException;
import br.com.sicape.api.infrastructure.settings.Settings;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiErrorHandler {

    private final Settings settings;

    public ApiErrorHandler(Settings settings) {
        this.settings = settings;
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handle(
        DomainException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            exception.getHttpStatus(),
            exception.getMessage(),
            exception,
            request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
        Exception exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Ocorreu um erro interno.",
            exception,
            request
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
        HttpStatus status,
        String message,
        Exception exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(status).body(
            new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                getDebug(exception)
            )
        );
    }

    private ApiErrorDebug getDebug(Exception exception) {

        if (!this.settings.getMode().isDevelopment()) {
            return null;
        }

        return new ApiErrorDebug(
            exception.getClass().getName(),
            exception.getMessage(),
            Arrays.stream(exception.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.toList())
        );
    }
}