package br.com.sicape.api.application.group.usecase;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.common.dto.response.PageResponse;
import br.com.sicape.api.application.group.dto.response.GroupListItemResponse;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Group;
import br.com.sicape.api.domain.enums.GroupStatus;
import br.com.sicape.api.domain.repository.GroupParticipantCount;
import br.com.sicape.api.domain.repository.GroupRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListGroupUseCase {
    private final GroupRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<GroupListItemResponse> execute(
        String name,
        String subject,
        GroupStatus status,
        int page,
        int size,
        AuthContext authContext
    ) {
        Page<Group> result = repository.search(
            authContext.district(),
            normalize(name),
            normalize(subject),
            status,
            PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))
        );

        Map<UUID, Long> participantCounts = result.getContent().isEmpty()
            ? Map.of()
            : repository.countParticipantsByGroupUuids(result.getContent().stream()
                .map(Group::getUuid)
                .toList())
                .stream()
                .collect(Collectors.toMap(
                    GroupParticipantCount::getGroupUuid,
                    GroupParticipantCount::getParticipantCount
                ));

        return new PageResponse<>(
            result.getContent().stream().map(group -> new GroupListItemResponse(
                group.getUuid(),
                group.getName(),
                group.getSubject(),
                group.getPresenters() == null ? java.util.List.of() : java.util.List.copyOf(group.getPresenters()),
                group.getStatus(),
                participantCounts.getOrDefault(group.getUuid(), 0L)
            )).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}