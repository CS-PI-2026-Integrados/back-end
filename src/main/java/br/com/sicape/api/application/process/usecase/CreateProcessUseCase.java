package br.com.sicape.api.application.process.usecase;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.application.process.dto.request.CreateProcessRequest;
import br.com.sicape.api.application.process.dto.response.ProcessListItemResponse;
import br.com.sicape.api.domain.entity.JudicialProcess;
import br.com.sicape.api.domain.enums.ProcessStatus;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.repository.JudicialProcessRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateProcessUseCase {
    private final JudicialProcessRepository repository;

    @Transactional
    public ProcessListItemResponse execute(CreateProcessRequest request, AuthContext auth) {
        String number = request.number() == null ? "" : request.number().trim();
        if (!number.matches("(?:[0-9]{20}|[0-9]{7}-[0-9]{2}\\.[0-9]{4}\\.[0-9]\\.[0-9]{2}\\.[0-9]{4})")) {
            throw new ValidationException("number", "Informe o número no formato 0000000-00.0000.0.00.0000.");
        }
        String digits = JudicialProcess.normalizeNumber(number);
        if (repository.existsByNormalizedNumber(digits)) {
            throw duplicate();
        }
        String formatted = digits.substring(0, 7) + "-" + digits.substring(7, 9) + "."
            + digits.substring(9, 13) + "." + digits.substring(13, 14) + "."
            + digits.substring(14, 16) + "." + digits.substring(16);
        JudicialProcess process;
        try {
            process = repository.saveAndFlush(new JudicialProcess(formatted, ProcessStatus.ACTIVE, auth.district()));
        } catch (DataIntegrityViolationException exception) {
            // Concurrent creations share the canonical number protected by the existing unique constraint.
            throw duplicate();
        }
        return new ProcessListItemResponse(process.getUuid(), process.getNumber(), process.getStatus(), 0, List.of());
    }

    private ConflictException duplicate() {
        return new ConflictException("number", "Já existe um processo com este número. Pesquise um processo ativo disponível nesta comarca.");
    }
}
