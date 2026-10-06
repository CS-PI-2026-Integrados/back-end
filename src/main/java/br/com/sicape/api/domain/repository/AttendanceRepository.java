package br.com.sicape.api.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import br.com.sicape.api.domain.entity.Attendance;
import br.com.sicape.api.domain.entity.JudicialDistrict;

public interface AttendanceRepository extends BaseRepository<Attendance> {
    @EntityGraph(attributePaths = {"photo", "receipt", "convicted", "process", "user"})
    Optional<Attendance> findByUuidAndDistrict(UUID uuid, JudicialDistrict district);

    @EntityGraph(attributePaths = {"convicted", "process", "user"})
    @Query(
        value = """
            select a from Attendance a
            where a.district = :district
              and (
                :search = ''
                or lower(a.convicted.name) like concat('%', :search, '%')
                or (:digits <> '' and a.convicted.cpf.value like concat('%', :digits, '%'))
                or (:digits <> '' and a.process.normalizedNumber like concat('%', :digits, '%'))
                or lower(a.process.number) like concat('%', :search, '%')
              )
            """,
        countQuery = """
            select count(a) from Attendance a
            where a.district = :district
              and (
                :search = ''
                or lower(a.convicted.name) like concat('%', :search, '%')
                or (:digits <> '' and a.convicted.cpf.value like concat('%', :digits, '%'))
                or (:digits <> '' and a.process.normalizedNumber like concat('%', :digits, '%'))
                or lower(a.process.number) like concat('%', :search, '%')
              )
            """
    )
    Page<Attendance> search(
        @Param("district") JudicialDistrict district,
        @Param("search") String search,
        @Param("digits") String digits,
        Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"photo", "receipt", "convicted", "process", "user"})
    @Query("select a from Attendance a where a.uuid = :uuid and a.district = :district")
    Optional<Attendance> findForReceipt(@Param("uuid") UUID uuid, @Param("district") JudicialDistrict district);
}
