package br.com.sicape.api.application.user.usecase;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import br.com.sicape.api.application.common.dto.response.PageResponse;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.application.user.dto.response.UserResponse;
import br.com.sicape.api.application.user.mapper.UserResponseMapper;
import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.exception.ForbiddenException;
import br.com.sicape.api.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListUsersUseCase {

    private final UserRepository userRepository;
    private final UserResponseMapper userResponseMapper;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> execute(String search, int page, int size, AuthContext authContext) {
        if (authContext == null || authContext.user() == null || authContext.user().getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Apenas administradores podem acessar a listagem de usuários.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<User> usersPage = userRepository.searchUsers(search, pageable);

        List<UserResponse> content = usersPage.getContent().stream()
            .map(userResponseMapper::toResponse)
            .toList();

        return new PageResponse<>(
            content,
            usersPage.getNumber(),
            usersPage.getSize(),
            usersPage.getTotalElements(),
            usersPage.getTotalPages()
        );
    }
}
