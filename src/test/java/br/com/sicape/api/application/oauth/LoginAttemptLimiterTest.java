package br.com.sicape.api.application.oauth;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import br.com.sicape.api.domain.exception.DomainException;
import org.springframework.http.HttpStatus;

class LoginAttemptLimiterTest {
    @Test
    void limitsAttemptsPerAccountAndAllowsRetryAfterWindow() {
        AtomicLong time = new AtomicLong(0);
        var limiter = new LoginAttemptLimiter(time::get);
        for (int i = 0; i < 10; i++) limiter.check("account-a");
        assertThatThrownBy(() -> limiter.check("account-a"))
            .isInstanceOfSatisfying(DomainException.class,
                error -> assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        assertThatCode(() -> limiter.check("account-b")).doesNotThrowAnyException();
        time.set(60_000);
        assertThatCode(() -> limiter.check("account-a")).doesNotThrowAnyException();
    }
}
