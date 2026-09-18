package br.com.sicape.api.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import br.com.sicape.api.domain.entity.Attendance;
import br.com.sicape.api.domain.entity.JudicialDistrict;

public interface AttendanceRepository extends BaseRepository<Attendance> {
    @EntityGraph(attributePaths = {"photo", "receipt"})
    Optional<Attendance> findByUuidAndDistrict(UUID uuid, JudicialDistrict district);
}
