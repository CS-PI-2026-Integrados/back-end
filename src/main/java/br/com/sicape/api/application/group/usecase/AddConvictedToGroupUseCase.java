package br.com.sicape.api.application.group.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.group.dto.response.GroupResponse;
import br.com.sicape.api.application.group.service.GroupFinder;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Group;
import br.com.sicape.api.domain.enums.GroupStatus;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.repository.GroupRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddConvictedToGroupUseCase {
    private final GroupFinder groupFinder;
    private final ConvictedFinder convictedFinder;
    private final GroupRepository groupRepository;

    @Transactional
    public void execute(UUID groupUuid, UUID convictedUuid, AuthContext authContext) {
        Group group = groupFinder.find(groupUuid, authContext);
        validatePlanned(group);
        group.addConvicted(convictedFinder.find(convictedUuid, authContext));
        groupRepository.save(group);
    }

    private void validatePlanned(Group group) {
        if (group.getStatus() != GroupStatus.PLANNED) {
            throw new ValidationException("status", "Só é possível alterar participantes de grupos planejados.");
        }
    }
}