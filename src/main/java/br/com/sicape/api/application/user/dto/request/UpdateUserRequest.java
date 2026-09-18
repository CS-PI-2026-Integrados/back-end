package br.com.sicape.api.application.user.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

import br.com.sicape.api.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRequest(
    @NotBlank(message = "O nome é obrigatório")
    String name,

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Informe um e-mail válido.")
    String email,

    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
        message = "A senha deve conter no mínimo 8 caracteres, incluindo letras e números."
    )
    String password,

    @NotNull(message = "O perfil de acesso é obrigatório")
    @JsonAlias({"role", "role_key", "roleKey"})
    UserRole role
) {}
