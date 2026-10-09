package br.com.sicape.api.domain.repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
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
    @Query("""
        select new br.com.sicape.api.domain.repository.RecentAttendance(
            a.uuid, a.convicted.uuid, a.convicted.name, a.createdAt
        )
        from Attendance a
        where a.district = :district and a.createdAt < :end
        order by a.createdAt desc, a.id desc
        """)
    List<RecentAttendance> findRecentActivities(
        @Param("district") JudicialDistrict district,
        @Param("end") Instant end,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"photo", "receipt", "convicted", "process", "user"})
    Optional<Attendance> findByUuidAndDistrict(UUID uuid, JudicialDistrict district);

    @EntityGraph(attributePaths = {"convicted", "process", "user"})
    @Query(
        value = """
            select a from Attendance a
            where a.district = :district
              and (:start is null or a.createdAt >= :start)
              and (:end is null or a.createdAt < :end)
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
              and (:start is null or a.createdAt >= :start)
              and (:end is null or a.createdAt < :end)
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
        @Param("start") Instant start,
        @Param("end") Instant end,
        Pageable pageable
    );

    interface PeriodBounds {
        Instant getFirstCreatedAt();
        Instant getLastCreatedAt();
    }

    @Query("select min(a.createdAt) as firstCreatedAt, max(a.createdAt) as lastCreatedAt from Attendance a where a.district = :district")
    PeriodBounds findPeriodBounds(@Param("district") JudicialDistrict district);

    long countByDistrictAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        JudicialDistrict district, Instant start, Instant end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"photo", "receipt", "convicted", "process", "user"})
    @Query("select a from Attendance a where a.uuid = :uuid and a.district = :district")
    Optional<Attendance> findForReceipt(@Param("uuid") UUID uuid, @Param("district") JudicialDistrict district);
}
