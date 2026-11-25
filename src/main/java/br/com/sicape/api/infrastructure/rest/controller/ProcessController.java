package br.com.sicape.api.infrastructure.rest.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sicape.api.application.common.dto.response.PageResponse;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.application.process.dto.request.CreateProcessRequest;
import br.com.sicape.api.application.process.dto.response.ProcessListItemResponse;
import br.com.sicape.api.application.process.usecase.CreateProcessUseCase;
import br.com.sicape.api.application.process.usecase.ListProcessUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/processes")
public class ProcessController {
    private final ListProcessUseCase listUseCase;
    private final CreateProcessUseCase createUseCase;

    @PostMapping
    public ResponseEntity<ProcessListItemResponse> create(
        @Valid @RequestBody CreateProcessRequest request,
        @AuthenticationPrincipal AuthContext auth
    ) {
        var response = createUseCase.execute(request, auth);
        return ResponseEntity.created(URI.create("/api/processes/" + response.id())).body(response);
    }

    @GetMapping
    public PageResponse<ProcessListItemResponse> list(
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @AuthenticationPrincipal AuthContext authContext
    ) {
        return listUseCase.execute(search, page, size, authContext);
    }
}
