package br.com.sicape.api.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.sicape.api.application.oauth.OauthJwtService;
import br.com.sicape.api.domain.entity.Session;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class OauthJwtServiceImpl implements OauthJwtService {
    
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.access-token-duration}")
    private Duration accessTokenDuration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String createAccessToken(Session session) {

        Instant now = Instant.now();

        return Jwts.builder()
            .issuer(issuer)
            .subject(session.getUser().getUuid().toString())
            .id(session.getUuid().toString())
            .claim("type", "access")
            .claim("user", Map.of(
                "id", session.getUser().getUuid().toString(),
                "name", session.getUser().getName(),
                "cpf", session.getUser().getCpf().value()
            ))
            .claim("judicialDistrict", Map.of(
                "id", session.getUser().getDistrict().getUuid().toString(),
                "name", session.getUser().getDistrict().getName()
            ))
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(accessTokenDuration)))
            .signWith(getKey())
            .compact();
    }

    @Override
    public String createRefreshToken(Session session) {

        Instant now = Instant.now();

        return Jwts.builder()
            .issuer(issuer)
            .subject(session.getUser().getUuid().toString())
            .id(session.getUuid().toString())
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(session.getExpiresAt()))
            .signWith(getKey())
            .compact();
    }

    @Override
    public Duration getAccessTokenDuration() {
        return accessTokenDuration;
    }

    @Override
    public Jws<Claims> parse(String token) {
        return Jwts.parser()
            .verifyWith(getKey())
            .requireIssuer(issuer)
            .build()
            .parseSignedClaims(token);
    }
}
