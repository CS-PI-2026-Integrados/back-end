package br.com.sicape.api.application.group.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateGroupRequest (
    @NotBlank(message = "O nome do grupo é obrigatório")
    String name,

    @NotBlank(message = "A descrição do grupo é obrigatória")
    String description

    
) {}