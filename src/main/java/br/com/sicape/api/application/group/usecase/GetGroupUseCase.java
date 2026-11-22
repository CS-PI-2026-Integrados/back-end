package br.com.sicape.api.application.group.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.group.dto.response.GroupResponse;
import br.com.sicape.api.application.group.dto.response.GroupParticipantMembershipResponse;
import br.com.sicape.api.application.group.service.GroupFinder;
import br.com.sicape.api.application.group.service.GroupParticipantMembershipService;
import br.com.sicape.api.application.oauth.AuthContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetGroupUseCase {
    private final GroupFinder finder;
    private final GroupParticipantMembershipService membershipService;

    @Transactional
    public GroupResponse execute(UUID uuid, AuthContext authContext) {
        var group = finder.findForUpdate(uuid, authContext);
        var memberships = membershipService.getAndBackfill(group).stream()
            .map(GroupParticipantMembershipResponse::from)
            .toList();
        return GroupResponse.from(group, memberships);
    }
}