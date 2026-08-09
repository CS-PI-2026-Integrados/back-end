package br.com.sicape.api.application.oauth;

import java.time.Duration;

import br.com.sicape.api.domain.entity.Session;
import br.com.sicape.api.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

public interface OauthJwtService {
    String createAccessToken(User user, Session session);
    String createRefreshToken(Session session);
    Duration getAccessTokenDuration();
    Jws<Claims> parse(String token);
}
