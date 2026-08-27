package br.com.sicape.api.domain.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends DomainException {
    private final String field;

    public ConflictException(String message) {
        super(message);
        this.field = null;
    }

    public ConflictException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
