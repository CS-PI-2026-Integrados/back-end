package br.com.sicape.api.application.oauth.login;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.sicape.api.application.oauth.OauthJwtService;
import br.com.sicape.api.domain.entity.Session;
import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.exception.InvalidCredentialsException;
import br.com.sicape.api.domain.repository.SessionRepository;
import br.com.sicape.api.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateSessionUseCase {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final OauthJwtService oauthJwtService;

    public CreateSessionResponse execute(CreateSessionRequest request)
    {
        Optional<User> optionalUser = userRepository.findByCpf(request.clientId());

        if (optionalUser.isEmpty()) {
            throw new InvalidCredentialsException();
        }

        User user = optionalUser.get();
        
        if (!passwordEncoder.matches(request.clientSecret(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        Session session = new Session();
        session.setUser(user);
        session.setExpiresAt(ZonedDateTime.now(ZoneOffset.UTC).plusMonths(3).toInstant());

        sessionRepository.save(session);

        return new CreateSessionResponse(
            "Bearer",
            oauthJwtService.createAccessToken(session),
            oauthJwtService.createRefreshToken(session),
            oauthJwtService.getAccessTokenDuration().toSeconds()
        );
    }
}
