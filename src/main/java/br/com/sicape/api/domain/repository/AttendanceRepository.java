package br.com.sicape.api.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import br.com.sicape.api.domain.entity.Attendance;
import br.com.sicape.api.domain.entity.JudicialDistrict;

public interface AttendanceRepository extends BaseRepository<Attendance> {
    @EntityGraph(attributePaths = {"photo", "receipt"})
    Optional<Attendance> findByUuidAndDistrict(UUID uuid, JudicialDistrict district);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"photo", "receipt", "convicted", "process", "user"})
    @Query("select a from Attendance a where a.uuid = :uuid and a.district = :district")
    Optional<Attendance> findForReceipt(@Param("uuid") UUID uuid, @Param("district") JudicialDistrict district);
}
