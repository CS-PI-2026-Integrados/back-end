package br.com.sicape.api.application.user.dto.response;

import java.time.Instant;
import java.util.UUID;

import br.com.sicape.api.domain.enums.UserRole;

public record UserResponse(
    UUID id,
    String name,
    String cpf,
    String email,
    UserRole role,
    UUID districtId,
    boolean isActive,
    boolean mustChangePassword,
    Instant createdAt,
    Instant lastAccessAt
) {}
