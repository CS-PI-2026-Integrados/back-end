package br.com.sicape.api.domain.repository;

import java.util.UUID;

public interface GroupParticipantCount {
    UUID getGroupUuid();

    long getParticipantCount();
}