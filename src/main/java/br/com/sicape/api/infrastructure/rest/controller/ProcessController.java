package br.com.sicape.api.infrastructure.rest.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sicape.api.application.common.dto.response.PageResponse;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.application.process.dto.response.ProcessListItemResponse;
import br.com.sicape.api.application.process.usecase.ListProcessUseCase;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/processes")
public class ProcessController {
    private final ListProcessUseCase listUseCase;

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
