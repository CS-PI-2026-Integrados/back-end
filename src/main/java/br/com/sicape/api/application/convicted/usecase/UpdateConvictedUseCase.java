package br.com.sicape.api.application.convicted.usecase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.convicted.dto.request.ConvictedProcessRequest;
import br.com.sicape.api.application.convicted.dto.request.UpdateConvictedRequest;
import br.com.sicape.api.application.convicted.dto.response.ConvictedResponse;
import br.com.sicape.api.application.convicted.mapper.ConvictedResponseMapper;
import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.ConvictedProcess;
import br.com.sicape.api.domain.entity.JudicialProcess;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.repository.JudicialProcessRepository;
import br.com.sicape.api.domain.valueobject.Cpf;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateConvictedUseCase {
    private final ConvictedFinder finder;
    private final ConvictedRepository repository;
    private final JudicialProcessRepository processRepository;
    private final ConvictedResponseMapper mapper;

    @Transactional
    public ConvictedResponse execute(UUID uuid, UpdateConvictedRequest request, AuthContext authContext) {
        Convicted convicted = finder.find(uuid, authContext);

        if (request.isNameProvided()) {
            requireText(request.getName(), "name");
            convicted.updateName(request.getName());
        }
        if (request.isCpfProvided()) {
            Cpf cpf = toCpf(request.getCpf());
            if (repository.existsByCpfAndUuidNot(cpf, uuid)) {
                throw new ConflictException("Já existe um condenado cadastrado com este CPF.");
            }
            convicted.updateCpf(cpf);
        }
        if (request.isBirthDateProvided()) {
            LocalDate birthDate = request.getBirthDate();
            if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
                throw new ValidationException("birth_date", "A data de nascimento não pode ser nula ou futura");
            }
            convicted.updateBirthDate(birthDate);
        }
        if (request.isPhoneProvided()) {
            requireText(request.getPhone(), "phone");
            convicted.updatePhone(request.getPhone());
        }
        if (request.isAddressProvided()) {
            if (request.getAddress() == null) {
                throw new ValidationException("address", "O endereço não pode ser nulo");
            }
            try {
                convicted.updateAddress(request.getAddress().apply(convicted.getAddress()));
            } catch (IllegalArgumentException exception) {
                throw new ValidationException("address", exception.getMessage());
            }
        }
        if (request.isEmploymentStatusProvided()) {
            convicted.updateEmploymentStatus(request.getEmploymentStatus());
        }
        if (request.isProcessesProvided()) {
            replaceProcesses(convicted, request.getProcesses(), authContext);
        }

        return mapper.toResponse(repository.save(convicted));
    }

    private void replaceProcesses(
        Convicted convicted,
        List<ConvictedProcessRequest> requested,
        AuthContext authContext
    ) {
        if (requested == null) {
            throw new ValidationException("processes", "A lista de processos não pode ser nula");
        }
        if (requested.isEmpty()) {
            convicted.replaceProcesses(List.of());
            return;
        }
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

        List<ConvictedProcess> links = new ArrayList<>();
        for (ConvictedProcessRequest item : requested) {
            links.add(new ConvictedProcess(convicted, processes.get(item.id()), item.principal()));
        }

        convicted.replaceProcesses(List.of());
        repository.saveAndFlush(convicted);
        convicted.replaceProcesses(links);
    }

    private Cpf toCpf(String value) {
        try {
            return Cpf.of(value);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("cpf", exception.getMessage());
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field, "O campo não pode ser nulo ou vazio");
        }
    }
}
