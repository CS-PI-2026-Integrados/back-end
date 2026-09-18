package br.com.sicape.api.domain.repository;

import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.ConvictedProcess;
import br.com.sicape.api.domain.entity.JudicialProcess;

public interface ConvictedProcessRepository extends BaseRepository<ConvictedProcess> {
    boolean existsByConvictedAndProcess(Convicted convicted, JudicialProcess process);
}
