package br.com.sicape.api.application.oauth.login;

import br.com.sicape.api.domain.valueobject.Cpf;

public record CreateSessionRequest (
    Cpf clientId,
    String clientSecret
) {}
