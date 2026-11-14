package br.com.sicape.api.application.oauth;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import br.com.sicape.api.domain.exception.DomainException;

/** Bounded per-account throttle for this single-instance deployment. */
@Component
public class LoginAttemptLimiter {
    private record Window(long start, int attempts) {}
    private final Map<String, Window> windows = new HashMap<>();
    private final LongSupplier clock;

    public LoginAttemptLimiter() { this(System::currentTimeMillis); }
    LoginAttemptLimiter(LongSupplier clock) { this.clock = clock; }

    public synchronized void check(String account) {
        long now = clock.getAsLong();
        windows.entrySet().removeIf(entry -> now - entry.getValue().start() >= 60_000);
        Window window = windows.get(account);
        if ((window == null && windows.size() >= 10_000) || (window != null && window.attempts() >= 10)) {
            throw new DomainException("Muitas tentativas. Aguarde um minuto e tente novamente.") {
                @Override public HttpStatus getHttpStatus() { return HttpStatus.TOO_MANY_REQUESTS; }
            };
        }
        windows.put(account, window == null ? new Window(now, 1) : new Window(window.start(), window.attempts() + 1));
    }
}
