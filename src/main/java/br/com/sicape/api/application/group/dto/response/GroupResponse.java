package br.com.sicape.api.application.group.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import br.com.sicape.api.domain.entity.Group;
import br.com.sicape.api.domain.enums.GroupFrequency;

public record GroupResponse(
    UUID uuid,
    Instant createdAt,
    Instant updatedAt,
    String name,
    String description,
    Integer minimumMeetingsCount,
    Integer totalMeetingsCounts,
    GroupFrequency frequency,
    LocalTime meetingBaseTime,
    LocalDate startDate,
    LocalDate predictedEndDate,
    LocalDate realEndDate
) {
    public static GroupResponse from(Group group) {
        return new GroupResponse(
            group.getUuid(),
            group.getCreatedAt(),
            group.getUpdatedAt(),
            group.getName(),
            group.getDescription(),
            group.getMinimumMeetingsCount(),
            group.getTotalMeetingsCounts(),
            group.getFrequency(),
            group.getMeetingBaseTime(),
            group.getStartDate(),
            group.getPredictedEndDate(),
            group.getRealEndDate()
        );
    }
}