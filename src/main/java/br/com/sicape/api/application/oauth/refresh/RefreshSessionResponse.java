package br.com.sicape.api.application.oauth.refresh;

public record RefreshSessionResponse(
    String accessToken,
    long expiresIn
) {}
