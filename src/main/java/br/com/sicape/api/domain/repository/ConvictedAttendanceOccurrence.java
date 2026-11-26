package br.com.sicape.api.domain.repository;

import java.time.Instant;
import java.util.UUID;

public record ConvictedAttendanceOccurrence(
    UUID id, Instant attendedAt, UUID processId, boolean receiptIssued
) {}
