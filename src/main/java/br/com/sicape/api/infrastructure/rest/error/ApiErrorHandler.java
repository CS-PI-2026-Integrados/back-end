package br.com.sicape.api.infrastructure.rest.error;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import br.com.sicape.api.domain.exception.DomainException;
import br.com.sicape.api.domain.exception.ValidationException;
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
        List<ApiFieldError> fields = exception instanceof ValidationException validation
            ? validation.getFields().stream()
                .map(field -> new ApiFieldError(field.field(), field.message()))
                .toList()
            : List.of();

        return buildResponse(
            exception.getHttpStatus(),
            exception.getMessage(),
            exception,
            request,
            fields
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        List<ApiFieldError> fields = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new ApiFieldError(jsonField(error.getField()), error.getDefaultMessage()))
            .toList();
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Um ou mais campos são inválidos.", exception, request, fields);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodValidation(
        HandlerMethodValidationException exception,
        HttpServletRequest request
    ) {
        List<ApiFieldError> fields = exception.getParameterValidationResults().stream()
            .flatMap(result -> result.getResolvableErrors().stream()
                .map(error -> new ApiFieldError(result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
            .toList();
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Um ou mais parâmetros são inválidos.", exception, request, fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(
        Exception exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "O corpo da requisição está ausente ou possui formato inválido.",
            exception,
            request,
            List.of(new ApiFieldError("request", "Conteúdo ausente ou inválido"))
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUpload(
        MaxUploadSizeExceededException exception,
        HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "A foto deve possuir no máximo 5 MB.", exception, request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
        MethodArgumentTypeMismatchException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Um parâmetro possui formato inválido.",
            exception,
            request,
            List.of(new ApiFieldError(exception.getName(), "Formato inválido"))
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegrity(
        DataIntegrityViolationException exception,
        HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, "A operação viola uma restrição de unicidade ou integridade.", exception, request, List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
        IllegalArgumentException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.UNPROCESSABLE_ENTITY,
            exception.getMessage(),
            exception,
            request,
            List.of(new ApiFieldError("request", exception.getMessage()))
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
            request,
            List.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
        HttpStatus status,
        String message,
        Exception exception,
        HttpServletRequest request,
        List<ApiFieldError> fields
    ) {
        return ResponseEntity.status(status).body(
            new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fields.isEmpty() ? null : fields,
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

    private String jsonField(String field) {
        return field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
