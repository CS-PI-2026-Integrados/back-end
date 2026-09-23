package br.com.sicape.api.application.group.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import br.com.sicape.api.domain.enums.GroupFrequency;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateGroupRequest(
    @NotBlank(message = "O nome do grupo é obrigatório")
    String name,

    @NotBlank(message = "A descrição do grupo é obrigatória")
    String description,

    @NotBlank(message = "O tema do grupo é obrigatório")
    String subject,

    @NotNull(message = "A quantidade total de encontros é obrigatória")
    @Min(value = 1, message = "A quantidade total de encontros deve ser maior que zero")
    Integer totalMeetingsCount,

    @NotNull(message = "A quantidade mínima de encontros é obrigatória")
    @Min(value = 1, message = "A quantidade mínima de encontros deve ser maior que zero")
    Integer minimumMeetingsCount,

    @NotBlank(message = "A  hora da reunião é obrigatória")
    @Pattern(
        regexp = "^([01]?\\d|2[0-3]):[0-5]\\d$",
        message = "A hora da reunião deve estar no formato H:i"
    )
    String meetingBaseTime,

    @NotNull(message = "A frequência do grupo é obrigatória")
    GroupFrequency frequency,

    @NotNull(message = "A data de início é obrigatória")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate startDate,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate predictedEndDate,

    List<@NotNull(message = "Cada UUID de apenado deve ser informado") UUID> convictedUuids
) {
    @AssertTrue(message = "A quantidade mínima de encontros não pode ser maior que o total de encontros")
    public boolean isMinimumMeetingsCountValid() {
        return totalMeetingsCount == null || minimumMeetingsCount == null || minimumMeetingsCount <= totalMeetingsCount;
    }

    @AssertTrue(message = "A data prevista de término não pode ser anterior à data de início")
    public boolean isPredictedEndDateValid() {
        return predictedEndDate == null || startDate == null || !predictedEndDate.isBefore(startDate);
    }
}
