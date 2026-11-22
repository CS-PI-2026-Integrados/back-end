package br.com.sicape.api.application.group.usecase;

import java.time.LocalTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.group.dto.request.CreateGroupRequest;
import br.com.sicape.api.application.group.dto.response.GroupResponse;
import br.com.sicape.api.application.group.dto.response.GroupParticipantMembershipResponse;
import br.com.sicape.api.application.group.service.GroupParticipantMembershipService;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.Group;
import br.com.sicape.api.domain.enums.GroupStatus;
import br.com.sicape.api.domain.enums.ConvictedStatus;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.exception.ForbiddenException;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.repository.GroupRepository;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class CreateGroupUseCase {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private final GroupRepository repo;
    private final ConvictedRepository convictedRepository;
    private final GroupParticipantMembershipService membershipService;

    @Transactional
    public GroupResponse execute(
        CreateGroupRequest request,
        AuthContext auth
    ) {
        validatePermission(auth);

        List<UUID> convictedUuids = request.convictedUuids() == null ? List.of() : request.convictedUuids();
        List<UUID> uniqueIds = convictedUuids.stream().distinct().toList();
        if (uniqueIds.size() != convictedUuids.size()) {
            throw new ValidationException("convictedUuids", "A lista de apenados contém UUIDs duplicados");
        }

        List<Convicted> convicteds = List.of();
        if (!uniqueIds.isEmpty()) {
            convicteds = convictedRepository.findAllByUuidInAndDistrict(uniqueIds, auth.district());
            if (convicteds.size() != uniqueIds.size()
                || convicteds.stream().anyMatch(convicted -> convicted.getStatus() != ConvictedStatus.ACTIVE)) {
                throw new ValidationException(
                    "convictedUuids",
                    "Um ou mais apenados não existem ou não estão ativos nesta comarca"
                );
            }
        }

        Group group = new Group();

        group.setName(request.name());
        group.setDescription(request.description());
        group.setSubject(request.subject());
        group.setPresenters(new ArrayList<>(request.presenters()));
        group.setStatus(GroupStatus.PLANNED);
        group.setMinimumMeetingsCount(request.minimumMeetingsCount());
        group.setTotalMeetingsCounts(request.totalMeetingsCount());
        group.setFrequency(request.frequency());
        group.setMeetingBaseTime(request.meetingBaseTime() == null ? null : LocalTime.parse(request.meetingBaseTime()));
        group.setStartDate(request.startDate());
        group.setPredictedEndDate(request.predictedEndDate());
        group.setConvicteds(convicteds);
        group.setDistrict(auth.district());

        repo.save(group);

        var memberships = convicteds.stream()
            .map(convicted -> membershipService.start(
                group,
                convicted,
                group.getStartDate() == null ? LocalDate.now(BUSINESS_ZONE) : group.getStartDate()
            ))
            .map(GroupParticipantMembershipResponse::from)
            .toList();
        return GroupResponse.from(group, memberships);
    }

    private void validatePermission(AuthContext auth) {
        if (auth == null || auth.user() == null) {
            throw new ForbiddenException("Apenas administradores e operadores podem cadastrar grupos reflexivos.");
        }

        UserRole role = auth.user().getRole();
        if (role != UserRole.ADMIN && role != UserRole.OPERATOR) {
            throw new ForbiddenException("Apenas administradores e operadores podem cadastrar grupos reflexivos.");
        }
    }
}
