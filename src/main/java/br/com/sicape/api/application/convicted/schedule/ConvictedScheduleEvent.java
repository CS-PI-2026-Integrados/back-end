package br.com.sicape.api.application.convicted.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ConvictedScheduleEvent(
    UUID id,
    Origin origin,
    LocalDate date,
    LocalTime time,
    String title,
    Status status,
    UUID processId
) {
    public enum Origin { REFLECTION_GROUP, INDIVIDUAL_APPEARANCE }

    public enum Status { SCHEDULED, CANCELLED }
}
