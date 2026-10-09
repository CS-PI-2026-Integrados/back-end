package br.com.sicape.api.application.convicted.dto.response;

import java.time.Instant;

public record ConvictedMetricsResponse(
    long total,
    long active,
    long inactive,
    long withRecentAttendance,
    long withoutRecentAttendance,
    Instant calculatedAt
) {}
