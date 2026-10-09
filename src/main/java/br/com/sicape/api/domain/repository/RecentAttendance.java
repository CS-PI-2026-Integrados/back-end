package br.com.sicape.api.domain.repository;

import java.time.Instant;
import java.util.UUID;

public record RecentAttendance(UUID id, UUID convictedId, String convictedName, Instant createdAt) {}
