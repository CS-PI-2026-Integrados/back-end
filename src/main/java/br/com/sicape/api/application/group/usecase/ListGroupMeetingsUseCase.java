package br.com.sicape.api.application.group.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.group.dto.response.GroupMeetingResponse;
import br.com.sicape.api.application.group.service.GroupFinder;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Group;
import br.com.sicape.api.domain.repository.GroupMeetingRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListGroupMeetingsUseCase {
    private final GroupFinder groupFinder;
    private final GroupMeetingRepository meetingRepository;

    @Transactional(readOnly = true)
    public List<GroupMeetingResponse> execute(UUID groupUuid, AuthContext authContext) {
        Group group = groupFinder.find(groupUuid, authContext);
        return meetingRepository.findAllByGroupOrderByDateAscIdAsc(group).stream()
            .map(GroupMeetingResponse::from)
            .toList();
    }
}
