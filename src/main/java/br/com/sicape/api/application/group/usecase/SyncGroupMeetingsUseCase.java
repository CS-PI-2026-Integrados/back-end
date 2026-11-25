package br.com.sicape.api.application.group.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.group.dto.request.GroupMeetingSyncItemRequest;
import br.com.sicape.api.application.group.dto.response.GroupMeetingResponse;
import br.com.sicape.api.application.group.service.GroupFinder;
import br.com.sicape.api.application.group.service.GroupMeetingSyncService;
import br.com.sicape.api.application.group.service.GroupMutationAuthorizer;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Group;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SyncGroupMeetingsUseCase {
    private final GroupFinder groupFinder;
    private final GroupMutationAuthorizer authorizer;
    private final GroupMeetingSyncService syncService;

    @Transactional
    public List<GroupMeetingResponse> execute(
        UUID groupUuid,
        List<GroupMeetingSyncItemRequest> request,
        AuthContext authContext
    ) {
        authorizer.requireOperator(authContext);
        Group group = groupFinder.findForUpdate(groupUuid, authContext);
        return syncService.sync(group, request, authContext, false);
    }
}
