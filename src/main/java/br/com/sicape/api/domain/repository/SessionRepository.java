package br.com.sicape.api.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;

import br.com.sicape.api.domain.entity.Session;

public interface SessionRepository extends BaseRepository<Session> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Session s set s.revoked = true where s.user.uuid = :userUuid")
    int revokeByUserUuid(@org.springframework.data.repository.query.Param("userUuid") UUID userUuid);

    @Override
    @EntityGraph(attributePaths = {"user", "user.district"})
    Optional<Session> findByUuid(UUID uuid);
}
