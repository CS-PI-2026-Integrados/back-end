package br.com.sicape.api.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.JudicialProcess;
import br.com.sicape.api.domain.enums.ProcessStatus;

public interface JudicialProcessRepository extends BaseRepository<JudicialProcess> {
    Optional<JudicialProcess> findByUuidAndDistrictAndStatus(
        UUID uuid, JudicialDistrict district, ProcessStatus status
    );

    List<JudicialProcess> findAllByUuidInAndDistrict(
        Collection<UUID> uuids,
        JudicialDistrict district
    );

        @Query("""
                select process from JudicialProcess process
                where process.district = :district
                    and process.status = :status
                    and (
                        :search = ''
                        or lower(process.number) like lower(concat('%', :search, '%'))
                        or (:digits <> '' and process.normalizedNumber like concat('%', :digits, '%'))
                    )
                """)
        Page<JudicialProcess> search(
                @Param("district") JudicialDistrict district,
                @Param("status") ProcessStatus status,
                @Param("search") String search,
                @Param("digits") String digits,
                Pageable pageable
        );
}
