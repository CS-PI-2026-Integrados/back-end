package br.com.sicape.api.domain.repository;

import java.util.UUID;

public interface GroupParticipantPreview {
    UUID getGroupUuid();
    UUID getParticipantUuid();
    String getParticipantName();
}
