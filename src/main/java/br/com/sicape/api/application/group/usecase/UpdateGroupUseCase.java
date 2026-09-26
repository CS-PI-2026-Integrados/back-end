package br.com.sicape.api.application.group.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.group.dto.request.UpdateGroupRequest;
import br.com.sicape.api.application.group.dto.response.GroupResponse;
import br.com.sicape.api.application.group.service.GroupFinder;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Group;
import br.com.sicape.api.domain.enums.GroupStatus;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.exception.ForbiddenException;
import br.com.sicape.api.domain.exception.FieldViolation;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.repository.GroupRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateGroupUseCase {
    private final GroupFinder groupFinder;
    private final GroupRepository groupRepository;

    @Transactional
    public GroupResponse execute(UUID groupUuid, UpdateGroupRequest request, AuthContext authContext) {
        validatePermission(authContext);
        Group group = groupFinder.find(groupUuid, authContext);
        validateRequest(request, group);

        if (request.isNameProvided()) {
            group.setName(request.getName());
        }
        if (request.isSubjectProvided()) {
            group.setSubject(request.getSubject());
        }
        if (request.isPresentersProvided()) {
            group.setPresenters(new ArrayList<>(request.getPresenters()));
        }
        if (request.isPredictedEndDateProvided()) {
            group.setPredictedEndDate(request.getPredictedEndDate());
        }
        if (request.isStatusProvided()) {
            group.setStatus(request.getStatus());
        }
        if (request.isStartDateProvided()) {
            group.setStartDate(request.getStartDate());
        }

        return GroupResponse.from(groupRepository.save(group));
    }

    private void validatePermission(AuthContext authContext) {
        if (authContext == null || authContext.user() == null) {
            throw new ForbiddenException("Apenas administradores e operadores podem editar grupos reflexivos.");
        }

        UserRole role = authContext.user().getRole();
        if (role != UserRole.ADMIN && role != UserRole.OPERATOR) {
            throw new ForbiddenException("Apenas administradores e operadores podem editar grupos reflexivos.");
        }
    }

    private void validateRequest(UpdateGroupRequest request, Group group) {
        List<FieldViolation> violations = new ArrayList<>();

        if (!request.hasChanges()) {
            violations.add(new FieldViolation("request", "Informe ao menos um campo para atualizar"));
        }

        request.getUnsupportedFields().forEach(field -> violations.add(
            new FieldViolation(field, "Este campo não pode ser alterado por este endpoint")
        ));

        if (request.isNameProvided() && (request.getName() == null || request.getName().isBlank())) {
            violations.add(new FieldViolation("name", "O nome do grupo não pode estar vazio"));
        }

        if (request.isSubjectProvided() && (request.getSubject() == null || request.getSubject().isBlank())) {
            violations.add(new FieldViolation("subject", "O tema do grupo não pode estar vazio"));
        }

        if (request.isPresentersProvided()
            && (request.getPresenters() == null || request.getPresenters().isEmpty()
                || request.getPresenters().stream().anyMatch(presenter -> presenter == null || presenter.isBlank()))) {
            violations.add(new FieldViolation("presenters", "Informe pelo menos um ministrante para o grupo"));
        }

        if (request.isStatusProvided() && request.getStatus() == null) {
            violations.add(new FieldViolation("status", "O status do grupo não pode ser nulo"));
        }

        if (request.isStartDateProvided() && group.getStatus() != GroupStatus.PLANNED) {
            violations.add(new FieldViolation("start_date", "A data de início não pode ser alterada após o início das atividades"));
        }

        var effectiveStartDate = request.isStartDateProvided() ? request.getStartDate() : group.getStartDate();
        if (request.isPredictedEndDateProvided()
            && request.getPredictedEndDate() != null
            && effectiveStartDate != null
            && request.getPredictedEndDate().isBefore(effectiveStartDate)) {
            violations.add(new FieldViolation("predicted_end_date", "A data prevista de término não pode ser anterior à data de início"));
        }

        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }
    }
}