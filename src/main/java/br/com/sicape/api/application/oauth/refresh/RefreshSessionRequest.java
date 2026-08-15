package br.com.sicape.api.application.oauth.refresh;

public record RefreshSessionRequest(
    String refreshToken
) {}