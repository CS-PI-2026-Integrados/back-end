package br.com.sicape.api.application.group.dto.response;

import java.util.List;
import java.util.UUID;

import br.com.sicape.api.domain.enums.GroupStatus;

public record GroupListItemResponse(
    UUID uuid,
    String name,
    String subject,
    List<String> presenters,
    GroupStatus status,
    long participantCount
) {}