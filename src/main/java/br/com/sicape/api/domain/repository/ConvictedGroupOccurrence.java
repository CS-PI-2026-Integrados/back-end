package br.com.sicape.api.domain.repository;

import java.time.LocalDate;
import java.util.UUID;

import br.com.sicape.api.domain.enums.GroupMeetingStatus;
import br.com.sicape.api.domain.enums.GroupPresenceStatus;

public record ConvictedGroupOccurrence(
    UUID id, LocalDate date, String title, GroupMeetingStatus status,
    GroupPresenceStatus participationStatus
) {}
