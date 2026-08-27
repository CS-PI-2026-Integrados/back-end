package br.com.sicape.api.application.user.mapper;

import org.springframework.stereotype.Component;

import br.com.sicape.api.application.user.dto.response.UserResponse;
import br.com.sicape.api.domain.entity.User;

@Component
public class UserResponseMapper {
    public UserResponse toResponse(User user) {
        return new UserResponse(
            user.getUuid(),
            user.getName(),
            user.getCpf() != null ? user.getCpf().value() : null,
            user.getEmail(),
            user.getRole(),
            user.getDistrict() != null ? user.getDistrict().getUuid() : null,
            user.isActive(),
            user.isMustChangePassword(),
            user.getCreatedAt(),
            user.getLastAccessAt()
        );
    }
}
