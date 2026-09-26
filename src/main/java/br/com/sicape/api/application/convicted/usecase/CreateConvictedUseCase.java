package br.com.sicape.api.application.convicted.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import br.com.sicape.api.application.convicted.dto.request.ConvictedProcessRequest;
import br.com.sicape.api.application.convicted.dto.request.CreateConvictedRequest;
import br.com.sicape.api.application.convicted.dto.response.ConvictedResponse;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.repository.JudicialProcessRepository;
import br.com.sicape.api.domain.entity.ConvictedProcess;
import br.com.sicape.api.domain.entity.JudicialProcess;
import br.com.sicape.api.domain.enums.ProcessStatus;
import br.com.sicape.api.domain.valueobject.Address;
import br.com.sicape.api.domain.valueobject.Cpf;
import br.com.sicape.api.domain.valueobject.Phone;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateConvictedUseCase {
    private final ConvictedRepository repository;
    private final JudicialProcessRepository processRepository;

    @Transactional
    public ConvictedResponse execute(CreateConvictedRequest request, AuthContext authContext) {
        Cpf cpf = toCpf(request.cpf());
        Phone phone = toPhone(request.phone());
        if (repository.existsByCpf(cpf)) {
            throw new ConflictException("Já existe um condenado cadastrado com este CPF.");
        }

        Address address;
        try {
            address = request.address().toValueObject();
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("address", exception.getMessage());
        }

        Convicted convicted = new Convicted(
            request.name(),
            cpf,
            request.birthDate(),
            phone,
            address,
            request.employmentStatus(),
            authContext.district()
        );

        if (request.processes() != null && !request.processes().isEmpty()) {
            convicted.replaceProcesses(buildProcesses(request.processes(), convicted, authContext));
        }

        return ConvictedResponse.from(repository.save(convicted));
    }

    private List<ConvictedProcess> buildProcesses(
        List<ConvictedProcessRequest> requested,
        Convicted convicted,
        AuthContext authContext
    ) {
        if (requested.stream().anyMatch(item -> item == null || item.id() == null)) {
            throw new ValidationException("processes", "Todos os processos devem possuir um ID");
        }
        if (requested.stream().filter(ConvictedProcessRequest::principal).count() != 1) {
            throw new ValidationException("processes", "Informe exatamente um processo principal");
        }

        var uniqueIds = new HashSet<>(requested.stream().map(ConvictedProcessRequest::id).toList());
        if (uniqueIds.size() != requested.size()) {
            throw new ValidationException("processes", "A lista contém processos duplicados");
        }

        Map<UUID, JudicialProcess> processes = processRepository
            .findAllByUuidInAndDistrict(uniqueIds, authContext.district())
            .stream()
            .collect(Collectors.toMap(JudicialProcess::getUuid, Function.identity()));

        if (processes.size() != uniqueIds.size()) {
            throw new ValidationException("processes", "Um ou mais processos não existem nesta comarca");
        }
        if (processes.values().stream().anyMatch(process -> process.getStatus() != ProcessStatus.ACTIVE)) {
            throw new ValidationException("processes", "Somente processos ativos podem ser vinculados");
        }

        return requested.stream()
            .map(item -> new ConvictedProcess(convicted, processes.get(item.id()), item.principal()))
            .toList();
    }

    private Cpf toCpf(String value) {
        try {
            return Cpf.of(value);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("cpf", exception.getMessage());
        }
    }

    private Phone toPhone(String value) {
        try {
            return Phone.of(value);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("phone", exception.getMessage());
        }
    }
}
