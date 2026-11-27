package br.com.sicape.api.application.convicted.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import br.com.sicape.api.domain.enums.GroupPresenceStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ConvictedScheduleEvent(
    UUID id,
    Origin origin,
    LocalDate date,
    LocalTime time,
    String title,
    Status status,
    UUID processId,
    GroupPresenceStatus participationStatus,
    Boolean receiptIssued
) {
    public enum Origin { REFLECTION_GROUP, INDIVIDUAL_APPEARANCE }

    public enum Status { SCHEDULED, COMPLETED, CANCELLED }
}
