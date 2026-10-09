package br.com.sicape.api.application.attendance.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AttendanceMetricsResponse(
    @JsonProperty("last_7_days") long last7Days,
    List<MonthlyCount> monthlyCounts,
    List<RecentActivity> recentActivities,
    Instant calculatedAt
) {
    public record MonthlyCount(String month, long count) {}
    public record RecentActivity(UUID id, UUID convictedId, String convictedName, Instant createdAt) {}
}
