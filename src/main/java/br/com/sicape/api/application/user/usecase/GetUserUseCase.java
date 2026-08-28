package br.com.sicape.api.application.user.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.application.user.dto.response.UserResponse;
import br.com.sicape.api.application.user.mapper.UserResponseMapper;
import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.exception.ForbiddenException;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetUserUseCase {

    private final UserRepository userRepository;
    private final UserResponseMapper userResponseMapper;

    @Transactional(readOnly = true)
    public UserResponse execute(UUID id, AuthContext authContext) {
        if (authContext == null || authContext.user() == null || authContext.user().getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Apenas administradores podem acessar os detalhes de usuários.");
        }

        User user = userRepository.findByUuid(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        return userResponseMapper.toResponse(user);
    }
}
