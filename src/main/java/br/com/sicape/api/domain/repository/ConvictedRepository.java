package br.com.sicape.api.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.enums.ConvictedStatus;
import br.com.sicape.api.domain.valueobject.Cpf;

public interface ConvictedRepository extends BaseRepository<Convicted> {
    boolean existsByCpf(Cpf cpf);

    boolean existsByCpfAndUuidNot(Cpf cpf, UUID uuid);

    @EntityGraph(attributePaths = {"processes", "processes.process"})
    Optional<Convicted> findByUuidAndDistrictAndStatusNot(
        UUID uuid,
        JudicialDistrict district,
        ConvictedStatus status
    );

    @Query(
        value = """
            select distinct convicted from Convicted convicted
            left join convicted.processes link
            left join link.process process
            where convicted.district = :district
              and convicted.status = :status
              and (
                :search = ''
                or lower(convicted.name) like concat('%', :search, '%')
                or (:digits is not null and convicted.cpf.value like concat('%', :digits, '%'))
                or (:digits is not null and process.normalizedNumber like concat('%', :digits, '%'))
                or lower(process.number) like concat('%', :search, '%')
              )
            """,
        countQuery = """
            select count(distinct convicted.id) from Convicted convicted
            left join convicted.processes link
            left join link.process process
            where convicted.district = :district
              and convicted.status = :status
              and (
                :search = ''
                or lower(convicted.name) like concat('%', :search, '%')
                or (:digits is not null and convicted.cpf.value like concat('%', :digits, '%'))
                or (:digits is not null and process.normalizedNumber like concat('%', :digits, '%'))
                or lower(process.number) like concat('%', :search, '%')
              )
            """
    )
    Page<Convicted> search(
        @Param("district") JudicialDistrict district,
        @Param("status") ConvictedStatus status,
        @Param("search") String search,
        @Param("digits") String digits,
        Pageable pageable
    );

    @Query("""
        select new br.com.sicape.api.domain.repository.ProcessConvictedCount(process.uuid, count(distinct convicted.id))
        from Convicted convicted
        join convicted.processes link
        join link.process process
        where process.uuid in :processUuids
          and convicted.district = :district
          and convicted.status = :status
        group by process.uuid
        """)
    List<ProcessConvictedCount> countActiveByProcesses(
        @Param("processUuids") Collection<UUID> processUuids,
        @Param("district") JudicialDistrict district,
        @Param("status") ConvictedStatus status
    );
}
