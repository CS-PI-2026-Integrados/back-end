package br.com.sicape.api.application.group.usecase;

import java.util.UUID;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.group.service.GroupFinder;
import br.com.sicape.api.application.group.service.GroupMutationAuthorizer;
import br.com.sicape.api.application.group.service.GroupParticipantMembershipService;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Group;
import br.com.sicape.api.domain.enums.GroupStatus;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.repository.GroupRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RemoveConvictedFromGroupUseCase {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private final GroupFinder groupFinder;
    private final ConvictedFinder convictedFinder;
    private final GroupRepository groupRepository;
    private final GroupMutationAuthorizer authorizer;
    private final GroupParticipantMembershipService membershipService;

    @Transactional
    public void execute(UUID groupUuid, UUID convictedUuid, AuthContext authContext) {
        authorizer.requireOperator(authContext);
        Group group = groupFinder.findForUpdate(groupUuid, authContext);
        validateEditable(group);
        membershipService.getAndBackfill(group);
        var convicted = convictedFinder.find(convictedUuid, authContext);
        if (!group.getConvicteds().contains(convicted)) {
            throw new ResourceNotFoundException("O apenado não está vinculado ao grupo.");
        }
        membershipService.end(
            group,
            convicted,
            LocalDate.now(BUSINESS_ZONE)
        );
        group.removeConvicted(convicted);
        groupRepository.save(group);
    }

    private void validateEditable(Group group) {
        if (group.getStatus() != GroupStatus.PLANNED && group.getStatus() != GroupStatus.ACTIVE) {
            throw new ValidationException(
                "status",
                "Só é possível alterar participantes de grupos planejados ou em andamento."
            );
        }
    }
}
