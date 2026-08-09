package br.com.sicape.api.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;

import br.com.sicape.api.domain.entity.Session;

public interface SessionRepository extends BaseRepository<Session> {

    @Override
    @EntityGraph(attributePaths = {"user", "user.district"})
    Optional<Session> findByUuid(UUID uuid);
}
