package br.com.sicape.api.application.convicted.usecase;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.convicted.dto.response.ConvictedListItemResponse;
import br.com.sicape.api.application.convicted.dto.response.PageResponse;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.ConvictedProcess;
import br.com.sicape.api.domain.enums.ConvictedStatus;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.repository.ProcessConvictedCount;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListConvictedUseCase {
    private final ConvictedRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<ConvictedListItemResponse> execute(
        String query,
        int page,
        int size,
        AuthContext authContext
    ) {
        String search = query == null ? "" : query.trim().toLowerCase();
        String normalizedDigits = search.replaceAll("\\D", "");
        String digits = normalizedDigits.isBlank() ? null : normalizedDigits;

        Page<Convicted> result = repository.search(
            authContext.district(),
            ConvictedStatus.ACTIVE,
            search,
            digits,
            PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))
        );

        Collection<UUID> mainProcessUuids = result.getContent().stream()
            .map(this::mainProcess)
            .filter(java.util.Objects::nonNull)
            .map(link -> link.getProcess().getUuid())
            .distinct()
            .toList();

        Map<UUID, Long> counts = mainProcessUuids.isEmpty()
            ? Map.of()
            : repository.countActiveByProcesses(
                mainProcessUuids,
                authContext.district(),
                ConvictedStatus.ACTIVE
            ).stream().collect(Collectors.toMap(
                ProcessConvictedCount::processUuid,
                ProcessConvictedCount::count
            ));

        var content = result.getContent().stream().map(convicted -> {
            ConvictedProcess main = mainProcess(convicted);
            return new ConvictedListItemResponse(
                convicted.getUuid(),
                convicted.getName(),
                convicted.getCpf().masked(),
                convicted.getPhotoUrl(),
                main == null ? null : main.getProcess().getNumber(),
                main == null ? 0 : counts.getOrDefault(main.getProcess().getUuid(), 0L)
            );
        }).toList();

        return new PageResponse<>(
            content,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    private ConvictedProcess mainProcess(Convicted convicted) {
        return convicted.getProcesses().stream()
            .filter(ConvictedProcess::isPrincipal)
            .findFirst()
            .orElse(null);
    }
}
