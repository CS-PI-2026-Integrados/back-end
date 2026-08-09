package br.com.sicape.api.domain.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("Credenciais inválidas");
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}