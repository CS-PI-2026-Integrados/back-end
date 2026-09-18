package br.com.sicape.api.application.user.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.exception.ForbiddenException;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteUserUseCase {

    private final UserRepository userRepository;

    @Transactional
    public void execute(UUID id, AuthContext authContext) {
        if (authContext == null || authContext.user() == null || authContext.user().getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Apenas administradores podem remover usuários.");
        }

        User targetUser = userRepository.findByUuid(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (authContext.user().getUuid().equals(targetUser.getUuid())) {
            throw new ForbiddenException("Você não pode remover seu próprio usuário.");
        }

        targetUser.setActive(false);
        userRepository.save(targetUser);
    }
}
