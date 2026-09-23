package br.com.sicape.api.application.group.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import br.com.sicape.api.domain.entity.Group;
import br.com.sicape.api.domain.enums.GroupFrequency;
import br.com.sicape.api.domain.enums.GroupStatus;

public record GroupResponse(
    UUID uuid,
    Instant createdAt,
    Instant updatedAt,
    String name,
    String description,
    String subject,
    List<String> presenters,
    GroupStatus status,
    Integer minimumMeetingsCount,
    Integer totalMeetingsCounts,
    GroupFrequency frequency,
    LocalTime meetingBaseTime,
    LocalDate startDate,
    LocalDate predictedEndDate,
    LocalDate realEndDate,
    List<GroupConvictedResponse> convicteds
) {
    public static GroupResponse from(Group group) {
        return new GroupResponse(
            group.getUuid(),
            group.getCreatedAt(),
            group.getUpdatedAt(),
            group.getName(),
            group.getDescription(),
            group.getSubject(),
            group.getPresenters() == null ? List.of() : List.copyOf(group.getPresenters()),
            group.getStatus(),
            group.getMinimumMeetingsCount(),
            group.getTotalMeetingsCounts(),
            group.getFrequency(),
            group.getMeetingBaseTime(),
            group.getStartDate(),
            group.getPredictedEndDate(),
            group.getRealEndDate(),
            group.getConvicteds() == null ? List.of() : group.getConvicteds().stream()
                .map(convicted -> new GroupConvictedResponse(
                    convicted.getUuid(),
                    convicted.getName(),
                    convicted.getCpf().value()
                ))
                .toList()
        );
    }
}
