package br.com.sicape.api.infraestructure.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sicape.api.application.uptime.GetUptimeResponse;
import br.com.sicape.api.application.uptime.GetUptimeUseCase;

@RestController
@RequestMapping("/status")
public class UptimeController {
    private final GetUptimeUseCase useCase;

    public UptimeController(
        GetUptimeUseCase useCase
    ) {
        this.useCase = useCase;
    }

    @GetMapping
    public ResponseEntity<GetUptimeResponse> handle()
    {
        return ResponseEntity.ok(useCase.execute());
    }
}