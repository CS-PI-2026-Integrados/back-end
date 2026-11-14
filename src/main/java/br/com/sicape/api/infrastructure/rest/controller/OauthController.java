package br.com.sicape.api.infrastructure.rest.controller;

import br.com.sicape.api.application.oauth.login.CreateSessionRequest;
import br.com.sicape.api.application.oauth.login.CreateSessionResponse;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sicape.api.application.oauth.login.CreateSessionUseCase;
import br.com.sicape.api.application.oauth.refresh.RefreshSessionRequest;
import br.com.sicape.api.application.oauth.refresh.RefreshSessionResponse;
import br.com.sicape.api.application.oauth.refresh.RefreshSessionUseCase;
import br.com.sicape.api.domain.valueobject.Cpf;

@RestController
@RequiredArgsConstructor
@RequestMapping("/authentication")
public class OauthController {
    private final CreateSessionUseCase createSessionUseCase;
    private final RefreshSessionUseCase refreshSessionUseCase;
    private final br.com.sicape.api.application.oauth.ChangePasswordUseCase changePasswordUseCase;
    private final br.com.sicape.api.application.oauth.RevokeSessionUseCase revokeSessionUseCase;

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @org.springframework.security.core.annotation.AuthenticationPrincipal br.com.sicape.api.application.oauth.AuthContext auth
    ) {
        revokeSessionUseCase.execute(auth);
        return ResponseEntity.noContent().build();
    }

    public record PasswordChangeRequest(String currentPassword, String newPassword) {}

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(
        @org.springframework.web.bind.annotation.RequestBody PasswordChangeRequest request,
        @org.springframework.security.core.annotation.AuthenticationPrincipal br.com.sicape.api.application.oauth.AuthContext auth
    ) {
        changePasswordUseCase.execute(auth, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<CreateSessionResponse> login(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        if (authorization == null || !authorization.startsWith("Basic ")) {
            throw new br.com.sicape.api.domain.exception.InvalidCredentialsException();
        }

        try {
            String credentials = new String(Base64.getDecoder().decode(authorization.substring(6)), StandardCharsets.UTF_8);
            String[] parts = credentials.split(":", 2);
            if (parts.length != 2 || parts[1].isEmpty()) {
                throw new br.com.sicape.api.domain.exception.InvalidCredentialsException();
            }
            return ResponseEntity.ok(createSessionUseCase.execute(new CreateSessionRequest(Cpf.of(parts[0]), parts[1])));
        } catch (IllegalArgumentException exception) {
            throw new br.com.sicape.api.domain.exception.InvalidCredentialsException();
        }
    }

    @PostMapping(
        value = "/refresh",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ResponseEntity<RefreshSessionResponse> refresh(
        @RequestParam("refresh_token") String refreshToken
    ) {
        return ResponseEntity.ok(refreshSessionUseCase.execute(
            new RefreshSessionRequest(
                refreshToken
            )
        ));
    }
}
