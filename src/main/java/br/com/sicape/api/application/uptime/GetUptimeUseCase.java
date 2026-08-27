package br.com.sicape.api.application.uptime;

import org.springframework.stereotype.Service;

@Service
public class GetUptimeUseCase {

    private final long startedAtNanos = System.nanoTime();

    public GetUptimeResponse execute() {
        long uptime = (System.nanoTime() - startedAtNanos) / 1_000_000_000;

        return new GetUptimeResponse(
            uptime
        );
    }
}
