package br.com.sicape.api.infrastructure.rest.controller;

import br.com.sicape.api.application.oauth.login.CreateSessionRequest;
import br.com.sicape.api.application.oauth.login.CreateSessionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/login")
    public ResponseEntity<CreateSessionResponse> login(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        if (authorization == null || !authorization.startsWith("Basic ")) {
            throw new IllegalArgumentException(
                "Credenciais não informadas."
            );
        }

        String credentials = new String(Base64.getDecoder().decode(authorization.substring(6)), StandardCharsets.UTF_8);

        String[] parts = credentials.split(":", 2);

        return ResponseEntity.ok(
            createSessionUseCase.execute(new CreateSessionRequest(
                Cpf.of(parts[0]),
                parts[1]
            ))
        );
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