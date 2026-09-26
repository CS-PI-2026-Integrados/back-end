package br.com.sicape.api.application.process.usecase;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.com.sicape.api.application.common.dto.response.PageResponse;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.application.process.dto.response.ProcessListItemResponse;
import br.com.sicape.api.domain.entity.JudicialProcess;
import br.com.sicape.api.domain.enums.ProcessStatus;
import br.com.sicape.api.domain.repository.JudicialProcessRepository;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.repository.ProcessConvictedName;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListProcessUseCase {
    private final JudicialProcessRepository repository;
    private final ConvictedRepository convictedRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProcessListItemResponse> execute(
        String query,
        int page,
        int size,
        AuthContext authContext
    ) {
        String search = query == null ? "" : query.trim();
        String digits = search.replaceAll("\\D", "");

        var result = repository.search(
            authContext.district(),
            ProcessStatus.ACTIVE,
            search,
            digits,
            PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "number"))
        );

        List<UUID> processUuids = result.getContent().stream()
            .map(JudicialProcess::getUuid)
            .toList();
        Map<UUID, List<String>> linkedNames = new HashMap<>();
        if (!processUuids.isEmpty()) {
            for (ProcessConvictedName linked : convictedRepository.findActiveNamesByProcesses(
                processUuids,
                authContext.district(),
                br.com.sicape.api.domain.enums.ConvictedStatus.ACTIVE
            )) {
                linkedNames.computeIfAbsent(linked.processUuid(), ignored -> new java.util.ArrayList<>())
                    .add(linked.convictedName());
            }
        }

        return new PageResponse<>(
            result.getContent().stream()
                .map(process -> toResponse(process, linkedNames.getOrDefault(process.getUuid(), List.of())))
                .toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    private ProcessListItemResponse toResponse(JudicialProcess process, List<String> linkedNames) {
        return new ProcessListItemResponse(
            process.getUuid(),
            process.getNumber(),
            process.getStatus(),
            linkedNames.size(),
            linkedNames
        );
    }
}