package br.com.sicape.api.domain.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends DomainException {
    public ConflictException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
