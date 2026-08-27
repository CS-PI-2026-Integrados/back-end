package br.com.sicape.api.application.user.usecase;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.application.user.dto.request.CreateUserRequest;
import br.com.sicape.api.application.user.dto.response.UserResponse;
import br.com.sicape.api.application.user.mapper.UserResponseMapper;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.exception.ForbiddenException;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.repository.UserRepository;
import br.com.sicape.api.domain.valueobject.Cpf;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateUserUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserResponseMapper mapper;

    @Transactional
    public UserResponse execute(CreateUserRequest request, AuthContext authContext) {
        if (authContext == null || authContext.user() == null || authContext.user().getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Apenas administradores podem cadastrar novos usuários.");
        }

        Cpf cpf = toCpf(request.cpf());
        if (userRepository.existsByCpf(cpf)) {
            throw new ConflictException("cpf", "Já existe um usuário cadastrado com este CPF.");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("email", "Já existe um usuário cadastrado com este e-mail.");
        }

        JudicialDistrict district = authContext.district();
        if (district == null) {
            district = authContext.user().getDistrict();
        }

        User user = new User(
            request.name(),
            cpf,
            normalizedEmail,
            passwordEncoder.encode(request.password()),
            request.role(),
            district
        );

        return mapper.toResponse(userRepository.save(user));
    }

    private Cpf toCpf(String value) {
        try {
            return Cpf.of(value);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("cpf", exception.getMessage());
        }
    }
}
