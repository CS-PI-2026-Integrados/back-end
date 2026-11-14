package br.com.sicape.api.application.oauth;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import br.com.sicape.api.domain.exception.InvalidCredentialsException;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.repository.SessionRepository;
import br.com.sicape.api.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase {
    private final UserRepository users;
    private final SessionRepository sessions;
    private final PasswordEncoder encoder;
    private final LoginAttemptLimiter attempts;

    @Transactional
    public void execute(AuthContext auth, String currentPassword, String newPassword) {
        if (auth != null && auth.user() != null) attempts.check("password:" + auth.user().getUuid());
        if (auth == null || auth.user() == null || currentPassword == null ||
            !encoder.matches(currentPassword, auth.user().getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (newPassword == null || newPassword.length() < 12 ||
            newPassword.getBytes(StandardCharsets.UTF_8).length > 72 ||
            !newPassword.matches("(?s).*[A-Za-z].*") || !newPassword.matches("(?s).*\\d.*")) {
            throw new ValidationException("new_password", "Use de 12 a 72 bytes, incluindo letras e números.");
        }
        if (encoder.matches(newPassword, auth.user().getPasswordHash())) {
            throw new ValidationException("new_password", "Use uma senha diferente da atual.");
        }
        var user = auth.user();
        user.setPasswordHash(encoder.encode(newPassword));
        user.setMustChangePassword(false);
        users.save(user);
        sessions.revokeByUserUuid(user.getUuid());
    }
}
