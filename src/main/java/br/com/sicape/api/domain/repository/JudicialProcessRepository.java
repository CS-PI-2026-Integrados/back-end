package br.com.sicape.api.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

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
}
