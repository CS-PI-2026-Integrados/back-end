package br.com.sicape.api.application.user.usecase;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.application.user.dto.request.UpdateUserRequest;
import br.com.sicape.api.application.user.dto.response.UserResponse;
import br.com.sicape.api.application.user.mapper.UserResponseMapper;
import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.exception.ForbiddenException;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateUserUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserResponseMapper mapper;

    @Transactional
    public UserResponse execute(UUID id, UpdateUserRequest request, AuthContext authContext) {
        if (authContext == null || authContext.user() == null || authContext.user().getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Apenas administradores podem atualizar usuários.");
        }

        User targetUser = userRepository.findByUuid(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        // Bloqueia se o usuário estiver tentando alterar a própria role
        if (authContext.user().getUuid().equals(targetUser.getUuid()) && request.role() != targetUser.getRole()) {
            throw new ForbiddenException("Você não pode alterar seu próprio nível de acesso.");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailAndUuidNot(normalizedEmail, targetUser.getUuid())) {
            throw new ConflictException("email", "Já existe um usuário cadastrado com este e-mail.");
        }

        targetUser.setName(request.name().trim());
        targetUser.setEmail(normalizedEmail);
        targetUser.setRole(request.role());

        if (request.password() != null && !request.password().isBlank()) {
            targetUser.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return mapper.toResponse(userRepository.save(targetUser));
    }
}
