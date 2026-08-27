package br.com.sicape.api.domain.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

public class ValidationException extends DomainException {
    private final List<FieldViolation> fields;

    public ValidationException(String field, String message) {
        this(List.of(new FieldViolation(field, message)));
    }

    public ValidationException(List<FieldViolation> fields) {
        super("Um ou mais campos são inválidos.");
        this.fields = List.copyOf(fields);
    }

    public List<FieldViolation> getFields() {
        return fields;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
