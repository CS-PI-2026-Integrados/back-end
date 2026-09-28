package br.com.sicape.api.application.group.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.group.service.GroupFinder;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Group;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.exception.ForbiddenException;
import br.com.sicape.api.domain.repository.GroupRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteGroupUseCase {
    private final GroupFinder groupFinder;
    private final GroupRepository groupRepository;

    @Transactional
    public void execute(UUID groupUuid, AuthContext authContext) {
        validatePermission(authContext);
        Group group = groupFinder.find(groupUuid, authContext);
        group.setDeleted(true);
        groupRepository.save(group);
    }

    private void validatePermission(AuthContext authContext) {
        if (authContext == null || authContext.user() == null) {
            throw new ForbiddenException("Apenas administradores e operadores podem remover grupos reflexivos.");
        }

        UserRole role = authContext.user().getRole();
        if (role != UserRole.ADMIN && role != UserRole.OPERATOR) {
            throw new ForbiddenException("Apenas administradores e operadores podem remover grupos reflexivos.");
        }
    }
}