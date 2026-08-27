package br.com.sicape.api.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.JudicialProcess;

public interface JudicialProcessRepository extends BaseRepository<JudicialProcess> {
    List<JudicialProcess> findAllByUuidInAndDistrict(
        Collection<UUID> uuids,
        JudicialDistrict district
    );
}
