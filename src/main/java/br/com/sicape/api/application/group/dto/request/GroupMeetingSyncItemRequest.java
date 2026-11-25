package br.com.sicape.api.application.group.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import br.com.sicape.api.domain.enums.GroupMeetingStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GroupMeetingSyncItemRequest(
    UUID uuid,

    @NotNull(message = "A data do encontro é obrigatória")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate date,

    @NotBlank(message = "O tema do encontro é obrigatório")
    @Size(max = 500, message = "O tema do encontro deve ter no máximo 500 caracteres")
    String subject,

    @NotNull(message = "O status do encontro é obrigatório")
    GroupMeetingStatus status,

    Long version,

    @NotNull(message = "A lista de presenças é obrigatória")
    List<@NotNull(message = "Cada registro de presença deve ser informado") @Valid GroupMeetingAttendanceRequest> attendance
) {}
