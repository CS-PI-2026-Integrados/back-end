package br.com.sicape.api.application.oauth.login;

public record CreateSessionResponse (
    String accessToken,
    String refreshToken,
    long expiresIn
) {}
