package br.com.sicape.api.domain.exception;

import org.springframework.http.HttpStatus;

public class PayloadTooLargeException extends DomainException {
    public PayloadTooLargeException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.PAYLOAD_TOO_LARGE;
    }
}
