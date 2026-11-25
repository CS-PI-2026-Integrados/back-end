package br.com.sicape.api.application.group.dto.request;

import java.util.UUID;

import br.com.sicape.api.domain.enums.GroupJustificationType;
import br.com.sicape.api.domain.enums.GroupPresenceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GroupMeetingAttendanceRequest(
    @NotNull(message = "O UUID do apenado é obrigatório")
    UUID convictedUuid,

    @NotNull(message = "O status da presença é obrigatório")
    GroupPresenceStatus status,

    GroupJustificationType justificationType,

    @Size(max = 200, message = "A observação da justificação deve ter no máximo 200 caracteres")
    String justificationText
) {}
