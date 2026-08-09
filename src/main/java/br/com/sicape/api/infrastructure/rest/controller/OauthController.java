package br.com.sicape.api.infrastructure.rest.controller;

import br.com.sicape.api.application.oauth.login.CreateSessionRequest;
import br.com.sicape.api.application.oauth.login.CreateSessionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sicape.api.application.oauth.login.CreateSessionUseCase;

@RestController
@RequestMapping("/authentication")
public class OauthController {
    private final CreateSessionUseCase createSessionUseCase;

    public OauthController(
        CreateSessionUseCase createSessionUseCase
    ) {
        this.createSessionUseCase = createSessionUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<CreateSessionResponse> login(@Valid @RequestBody CreateSessionRequest request)
    {
        return ResponseEntity.ok(createSessionUseCase.execute(request));
    }
}