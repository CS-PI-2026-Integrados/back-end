package br.com.sicape.api.application.oauth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.sicape.api.domain.repository.SessionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RevokeSessionUseCase {
    private final SessionRepository sessions;

    @Transactional
    public void execute(AuthContext auth) {
        if (auth != null && auth.session() != null) {
            auth.session().setRevoked(true);
            sessions.save(auth.session());
        }
    }
}
