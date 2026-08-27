package br.com.sicape.api.infrastructure.rest.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sicape.api.application.common.dto.response.PageResponse;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.application.user.dto.request.CreateUserRequest;
import br.com.sicape.api.application.user.dto.response.UserResponse;
import br.com.sicape.api.application.user.usecase.CreateUserUseCase;
import br.com.sicape.api.application.user.usecase.GetUserUseCase;
import br.com.sicape.api.application.user.usecase.ListUsersUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping({"/usuarios", "/users"})
public class UserController {
    private final CreateUserUseCase createUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final GetUserUseCase getUserUseCase;

    @PostMapping
    public ResponseEntity<UserResponse> create(
        @Valid @RequestBody CreateUserRequest request,
        @AuthenticationPrincipal AuthContext authContext
    ) {
        UserResponse response = createUserUseCase.execute(request, authContext);
        return ResponseEntity.created(URI.create("/api/usuarios/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> list(
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @AuthenticationPrincipal AuthContext authContext
    ) {
        return ResponseEntity.ok(listUsersUseCase.execute(search, page, size, authContext));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(
        @PathVariable UUID id,
        @AuthenticationPrincipal AuthContext authContext
    ) {
        return ResponseEntity.ok(getUserUseCase.execute(id, authContext));
    }
}
