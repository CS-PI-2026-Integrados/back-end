package br.com.sicape.api.application.group.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.com.sicape.api.application.group.dto.request.GroupMeetingAttendanceRequest;
import br.com.sicape.api.application.group.dto.request.GroupMeetingSyncItemRequest;
import br.com.sicape.api.application.group.dto.response.GroupMeetingResponse;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.Group;
import br.com.sicape.api.domain.entity.GroupMeeting;
import br.com.sicape.api.domain.entity.GroupMeetingPresence;
import br.com.sicape.api.domain.entity.GroupParticipantMembership;
import br.com.sicape.api.domain.enums.GroupMeetingStatus;
import br.com.sicape.api.domain.enums.GroupPresenceStatus;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.exception.FieldViolation;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.repository.GroupMeetingRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupMeetingSyncService {
    private final GroupMeetingRepository meetingRepository;
    private final ConvictedRepository convictedRepository;
    private final GroupParticipantMembershipService membershipService;

    public List<GroupMeetingResponse> sync(
        Group group,
        List<GroupMeetingSyncItemRequest> items,
        AuthContext authContext,
        boolean importOnlyWhenEmpty
    ) {
        if (items == null) {
            throw new ValidationException("meetings", "A lista de encontros é obrigatória.");
        }

        List<GroupMeeting> existingMeetings = meetingRepository.findAllByGroupOrderByDateAscIdAsc(group);
        if (importOnlyWhenEmpty && !existingMeetings.isEmpty()) {
            return existingMeetings.stream().map(GroupMeetingResponse::from).toList();
        }

        validatePayload(items);

        List<GroupParticipantMembership> memberships = membershipService.getAndBackfill(group);
        Set<UUID> allowedConvictedUuids = collectAllowedConvictedUuids(group, memberships, existingMeetings);
        Map<UUID, Convicted> convictedByUuid = loadConvicteds(items, allowedConvictedUuids, authContext);

        Map<UUID, GroupMeeting> existingByUuid = existingMeetings.stream()
            .collect(Collectors.toMap(GroupMeeting::getUuid, Function.identity()));

        Set<UUID> requestedUuids = items.stream()
            .map(GroupMeetingSyncItemRequest::uuid)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        List<GroupMeeting> meetingsToDelete = new ArrayList<>();
        for (GroupMeeting existing : existingMeetings) {
            if (!requestedUuids.contains(existing.getUuid())) {
                if (existing.getStatus() != GroupMeetingStatus.PENDENTE || existing.hasPresences()) {
                    throw new ConflictException(
                        "meetings",
                        "Não é permitido remover encontros realizados, não realizados ou com presenças registradas."
                    );
                }
                meetingsToDelete.add(existing);
            }
        }

        List<GroupMeeting> meetingsToSave = new ArrayList<>();
        for (GroupMeetingSyncItemRequest item : items) {
            if (item.uuid() != null && existingByUuid.containsKey(item.uuid())) {
                GroupMeeting existing = existingByUuid.get(item.uuid());
                reconcileExisting(existing, item, group, convictedByUuid);
                meetingsToSave.add(existing);
            } else {
                meetingsToSave.add(createNewMeeting(item, group, convictedByUuid));
            }
        }

        if (!meetingsToDelete.isEmpty()) {
            meetingRepository.deleteAll(meetingsToDelete);
        }
        if (!meetingsToSave.isEmpty()) {
            meetingRepository.saveAll(meetingsToSave);
        }
        meetingRepository.flush();

        return meetingRepository.findAllByGroupOrderByDateAscIdAsc(group).stream()
            .map(GroupMeetingResponse::from)
            .toList();
    }

    private void validatePayload(List<GroupMeetingSyncItemRequest> items) {
        List<FieldViolation> violations = new ArrayList<>();
        Set<UUID> seenMeetingUuids = new HashSet<>();

        for (int i = 0; i < items.size(); i++) {
            GroupMeetingSyncItemRequest item = items.get(i);
            String prefix = "meetings[" + i + "]";
            if (item == null) {
                violations.add(new FieldViolation(prefix, "O encontro não pode ser nulo."));
                continue;
            }
            if (item.uuid() != null && !seenMeetingUuids.add(item.uuid())) {
                violations.add(new FieldViolation(prefix + ".uuid", "A lista de encontros contém UUIDs duplicados."));
            }
            if (item.date() == null) {
                violations.add(new FieldViolation(prefix + ".date", "A data do encontro é obrigatória."));
            }
            if (item.subject() == null || item.subject().isBlank()) {
                violations.add(new FieldViolation(prefix + ".subject", "O tema do encontro é obrigatório."));
            } else if (item.subject().trim().length() > 500) {
                violations.add(new FieldViolation(prefix + ".subject", "O tema do encontro deve ter no máximo 500 caracteres."));
            }
            if (item.status() == null) {
                violations.add(new FieldViolation(prefix + ".status", "O status do encontro é obrigatório."));
            }
            if (item.attendance() == null) {
                violations.add(new FieldViolation(prefix + ".attendance", "A lista de presenças é obrigatória."));
                continue;
            }
            if ((item.status() == GroupMeetingStatus.PENDENTE || item.status() == GroupMeetingStatus.CANCELADO)
                && !item.attendance().isEmpty()) {
                violations.add(new FieldViolation(
                    prefix + ".attendance",
                    item.status() == GroupMeetingStatus.PENDENTE
                        ? "Encontros pendentes não podem possuir presenças registradas."
                        : "Encontros marcados como não realizados não podem possuir presenças registradas."
                ));
            }
            validateAttendanceItems(prefix, item.attendance(), violations);
        }

        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }
    }

    private void validateAttendanceItems(
        String meetingPrefix,
        List<GroupMeetingAttendanceRequest> attendance,
        List<FieldViolation> violations
    ) {
        Set<UUID> seenConvicteds = new HashSet<>();
        for (int j = 0; j < attendance.size(); j++) {
            GroupMeetingAttendanceRequest record = attendance.get(j);
            String prefix = meetingPrefix + ".attendance[" + j + "]";
            if (record == null) {
                violations.add(new FieldViolation(prefix, "O registro de presença não pode ser nulo."));
                continue;
            }
            if (record.convictedUuid() == null) {
                violations.add(new FieldViolation(prefix + ".convicted_uuid", "O UUID do apenado é obrigatório."));
            } else if (!seenConvicteds.add(record.convictedUuid())) {
                violations.add(new FieldViolation(prefix + ".convicted_uuid", "Não repita o mesmo apenado no mesmo encontro."));
            }
            if (record.status() == null) {
                violations.add(new FieldViolation(prefix + ".status", "O status da presença é obrigatório."));
                continue;
            }
            String text = normalizeText(record.justificationText());
            if (text != null && text.length() > 200) {
                violations.add(new FieldViolation(prefix + ".justification_text", "A observação deve ter no máximo 200 caracteres."));
            }
            if (record.status() == GroupPresenceStatus.JUSTIFICADO) {
                if (record.justificationType() == null) {
                    violations.add(new FieldViolation(prefix + ".justification_type", "O tipo de justificação é obrigatório."));
                }
            } else if (record.justificationType() != null || text != null) {
                violations.add(new FieldViolation(
                    prefix + ".justification_type",
                    "Justificativa só pode ser informada quando o status for JUSTIFICADO."
                ));
            }
        }
    }

    private Set<UUID> collectAllowedConvictedUuids(
        Group group,
        List<GroupParticipantMembership> memberships,
        List<GroupMeeting> existingMeetings
    ) {
        Set<UUID> allowed = new HashSet<>();
        if (group.getConvicteds() != null) {
            group.getConvicteds().forEach(convicted -> allowed.add(convicted.getUuid()));
        }
        memberships.forEach(membership -> allowed.add(membership.getConvicted().getUuid()));
        existingMeetings.forEach(meeting ->
            meeting.getPresences().forEach(presence -> allowed.add(presence.getConvicted().getUuid()))
        );
        return allowed;
    }

    private Map<UUID, Convicted> loadConvicteds(
        List<GroupMeetingSyncItemRequest> items,
        Set<UUID> allowedConvictedUuids,
        AuthContext authContext
    ) {
        Set<UUID> requestedUuids = items.stream()
            .flatMap(item -> item.attendance().stream())
            .map(GroupMeetingAttendanceRequest::convictedUuid)
            .collect(Collectors.toSet());

        if (requestedUuids.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Convicted> found = convictedRepository
            .findAllByUuidInAndDistrict(requestedUuids, authContext.district())
            .stream()
            .collect(Collectors.toMap(Convicted::getUuid, Function.identity()));

        List<FieldViolation> violations = new ArrayList<>();
        for (UUID uuid : requestedUuids) {
            if (!found.containsKey(uuid)) {
                violations.add(new FieldViolation("convicted_uuid", "Apenado não encontrado nesta comarca: " + uuid));
            } else if (!allowedConvictedUuids.contains(uuid)) {
                violations.add(new FieldViolation("convicted_uuid", "O apenado não pertence a este grupo reflexivo: " + uuid));
            }
        }
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }
        return found;
    }

    private void reconcileExisting(
        GroupMeeting existing,
        GroupMeetingSyncItemRequest item,
        Group group,
        Map<UUID, Convicted> convictedByUuid
    ) {
        if (item.version() == null || !item.version().equals(existing.getVersion())) {
            throw new ConflictException(
                "version",
                "Os encontros deste grupo foram alterados por outro usuário. Recarregue a página antes de salvar."
            );
        }

        if (existing.getStatus() == GroupMeetingStatus.REALIZADO) {
            if (item.status() == GroupMeetingStatus.CANCELADO) {
                throw new ConflictException(
                    "status",
                    "Não é possível marcar como não realizado um encontro já realizado ou com presenças registradas."
                );
            }
            if (item.status() != GroupMeetingStatus.REALIZADO
                || !Objects.equals(existing.getDate(), item.date())
                || !Objects.equals(existing.getSubject(), item.subject().trim())
                || !sameAttendance(existing.getPresences(), item.attendance())) {
                throw new ConflictException(
                    "status",
                    "Encontros realizados preservam seu histórico e não podem ser alterados."
                );
            }
            return;
        }

        if (existing.getStatus() == GroupMeetingStatus.CANCELADO) {
            if (item.status() != GroupMeetingStatus.CANCELADO
                || !Objects.equals(existing.getDate(), item.date())
                || !Objects.equals(existing.getSubject(), item.subject().trim())
                || !item.attendance().isEmpty()) {
                throw new ConflictException(
                    "status",
                    "Encontros não realizados preservam seu histórico e não podem ser alterados."
                );
            }
            return;
        }

        if (existing.hasPresences() && item.status() == GroupMeetingStatus.CANCELADO) {
            throw new ConflictException(
                "status",
                "Não é possível marcar como não realizado um encontro com presenças registradas."
            );
        }

        existing.updateSchedule(item.date(), item.subject());
        if (item.status() == GroupMeetingStatus.REALIZADO) {
            existing.markRealized(buildPresences(existing, group, item.attendance(), convictedByUuid));
        } else if (item.status() == GroupMeetingStatus.CANCELADO) {
            existing.markNotRealized();
        }
    }

    private GroupMeeting createNewMeeting(
        GroupMeetingSyncItemRequest item,
        Group group,
        Map<UUID, Convicted> convictedByUuid
    ) {
        if (item.uuid() != null && meetingRepository.existsByUuidAndGroupNot(item.uuid(), group)) {
            throw new ConflictException("uuid", "Já existe um encontro cadastrado com este identificador.");
        }
        GroupMeeting meeting = new GroupMeeting(group, item.date(), item.subject());
        if (item.uuid() != null) {
            meeting.setUuid(item.uuid());
        }
        if (item.status() == GroupMeetingStatus.REALIZADO) {
            meeting.markRealized(buildPresences(meeting, group, item.attendance(), convictedByUuid));
        } else if (item.status() == GroupMeetingStatus.CANCELADO) {
            meeting.markNotRealized();
        }
        return meeting;
    }

    private List<GroupMeetingPresence> buildPresences(
        GroupMeeting meeting,
        Group group,
        List<GroupMeetingAttendanceRequest> items,
        Map<UUID, Convicted> convictedByUuid
    ) {
        List<GroupMeetingPresence> records = new ArrayList<>(items.size());
        for (GroupMeetingAttendanceRequest item : items) {
            Convicted convicted = convictedByUuid.get(item.convictedUuid());
            GroupParticipantMembership membership = membershipService.findAtDate(group, convicted, meeting.getDate());
            records.add(new GroupMeetingPresence(
                meeting,
                convicted,
                membership,
                item.status(),
                item.justificationType(),
                normalizeText(item.justificationText())
            ));
        }
        return records;
    }

    private boolean sameAttendance(
        List<GroupMeetingPresence> existing,
        List<GroupMeetingAttendanceRequest> incoming
    ) {
        if (existing.size() != incoming.size()) {
            return false;
        }
        Map<UUID, GroupMeetingPresence> existingByConvicted = new HashMap<>();
        for (GroupMeetingPresence presence : existing) {
            existingByConvicted.put(presence.getConvicted().getUuid(), presence);
        }
        for (GroupMeetingAttendanceRequest req : incoming) {
            GroupMeetingPresence match = existingByConvicted.get(req.convictedUuid());
            if (match == null
                || match.getStatus() != req.status()
                || match.getJustificationType() != req.justificationType()
                || !Objects.equals(match.getJustificationText(), normalizeText(req.justificationText()))) {
                return false;
            }
        }
        return true;
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
