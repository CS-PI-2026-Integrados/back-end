package br.com.sicape.api.application.oauth.refresh;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import br.com.sicape.api.application.oauth.OauthJwtService;
import br.com.sicape.api.domain.entity.Session;
import br.com.sicape.api.domain.exception.InvalidCredentialsException;
import br.com.sicape.api.domain.repository.SessionRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshSessionUseCase {

    private final SessionRepository sessionRepository;
    private final OauthJwtService oauthJwtService;

    public RefreshSessionResponse execute(RefreshSessionRequest request) {

        Claims claims = oauthJwtService
            .parse(request.refreshToken())
            .getPayload();

        if (!"refresh".equals(claims.get("type"))) {
            throw new InvalidCredentialsException();
        }

        UUID sessionUuid = UUID.fromString(claims.getId());

        Optional<Session> optionalSession =
            sessionRepository.findByUuid(sessionUuid);

        if (optionalSession.isEmpty()) {
            throw new InvalidCredentialsException();
        }

        Session session = optionalSession.get();

        if (session.isRevoked()) {
            throw new InvalidCredentialsException();
        }

        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = oauthJwtService.createAccessToken(session);

        return new RefreshSessionResponse(
            accessToken,
            oauthJwtService.getAccessTokenDuration().toSeconds()
        );
    }
}