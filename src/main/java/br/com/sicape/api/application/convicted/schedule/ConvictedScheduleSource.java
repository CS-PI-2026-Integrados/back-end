package br.com.sicape.api.application.convicted.schedule;

import java.time.LocalDate;
import java.util.List;

import br.com.sicape.api.domain.entity.Convicted;

public interface ConvictedScheduleSource {
    ConvictedScheduleEvent.Origin origin();

    /**
     * Reads authoritative occurrences for this person and their district in [from, to).
     * Group sources must apply membership as of the occurrence, and use the current date of rescheduled events.
     */
    List<ConvictedScheduleEvent> findEvents(Convicted convicted, LocalDate from, LocalDate to);
}
