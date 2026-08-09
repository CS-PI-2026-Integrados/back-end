package br.com.sicape.api.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção base para violações de regras do domínio.
 * A camada HTTP fará a tradução para a resposta apropriada quando necessário.
 */
public class DomainException extends RuntimeException
{
    public DomainException(String message) {
        super(message);
    }

    public HttpStatus getHttpStatus()
    {
        return HttpStatus.BAD_REQUEST;
    }
}
