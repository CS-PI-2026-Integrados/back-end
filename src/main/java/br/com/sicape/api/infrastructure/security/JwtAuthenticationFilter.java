package br.com.sicape.api.infrastructure.security;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.application.oauth.OauthJwtService;
import br.com.sicape.api.domain.entity.Session;
import br.com.sicape.api.domain.repository.SessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SessionRepository sessionRepository;
    private final OauthJwtService jwtService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (authorization != null && authorization.startsWith("Bearer ")) {

            try {
                String token = authorization.substring(7);

                var claims = jwtService.parse(token).getPayload();

                if (!"access".equals(claims.get("type", String.class)) || claims.getId() == null) {
                    filterChain.doFilter(request, response);
                    return;
                }

                UUID sessionUuid = UUID.fromString(claims.getId());

                Optional<Session> optionalSession = sessionRepository.findByUuid(sessionUuid);

                if (optionalSession.isPresent()) {
                    Session session = optionalSession.get();

                    if (session.isRevoked()) {
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }

                    if (session.getExpiresAt().isBefore(Instant.now())) {
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }

                    if (!session.getUser().isActive()) {
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }

                    AuthContext authContext = new AuthContext(
                        session.getUser(),
                        session.getUser().getDistrict(),
                        session
                    );

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        authContext,
                        null,
                        List.of()
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
