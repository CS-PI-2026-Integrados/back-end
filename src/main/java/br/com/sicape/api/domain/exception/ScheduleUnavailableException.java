package br.com.sicape.api.domain.exception;

import org.springframework.http.HttpStatus;

public class ScheduleUnavailableException extends DomainException {
    public ScheduleUnavailableException() {
        super("A agenda está temporariamente indisponível.");
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.SERVICE_UNAVAILABLE;
    }
}
