package br.com.sicape.api.application.group.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.Group;
import br.com.sicape.api.domain.entity.GroupParticipantMembership;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.repository.GroupParticipantMembershipRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupParticipantMembershipService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private final GroupParticipantMembershipRepository repository;

    @Transactional
    public GroupParticipantMembership start(Group group, Convicted convicted, LocalDate startDate) {
        if (repository.findByGroupIdAndConvictedIdAndEndDateIsNull(group.getId(), convicted.getId()).isPresent()) {
            throw new ConflictException("O apenado já possui vínculo ativo com este grupo.");
        }
        LocalDate effectiveStart = repository
            .findTopByGroupIdAndConvictedIdOrderByStartDateDesc(group.getId(), convicted.getId())
            .map(GroupParticipantMembership::getEndDate)
            .filter(java.util.Objects::nonNull)
            .map(lastEndDate -> startDate.isAfter(lastEndDate) ? startDate : lastEndDate.plusDays(1))
            .orElse(startDate);
        return repository.save(new GroupParticipantMembership(group, convicted, effectiveStart));
    }

    @Transactional
    public void end(Group group, Convicted convicted, LocalDate endDate) {
        GroupParticipantMembership membership = repository
            .findByGroupIdAndConvictedIdAndEndDateIsNull(group.getId(), convicted.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Vínculo ativo do apenado não encontrado."));
        if (endDate.isBefore(membership.getStartDate())) {
            repository.delete(membership);
            return;
        }
        membership.endOn(endDate);
        repository.save(membership);
    }

    @Transactional
    public void rescheduleInitialMemberships(Group group, LocalDate previousStartDate, LocalDate startDate) {
        LocalDate initialStartDate = previousStartDate != null
            ? previousStartDate
            : group.getCreatedAt() == null
                ? LocalDate.now(BUSINESS_ZONE)
                : group.getCreatedAt().atZone(BUSINESS_ZONE).toLocalDate();
        repository.findAllByGroupUuid(group.getUuid()).stream()
            .filter(membership -> membership.getEndDate() == null
                && membership.getStartDate().equals(initialStartDate))
            .forEach(membership -> membership.rescheduleStartDate(startDate));
    }

    @Transactional(readOnly = true)
    public GroupParticipantMembership findAtDate(Group group, Convicted convicted, LocalDate date) {
        List<GroupParticipantMembership> matches =
            repository.findMembershipAtDate(group.getUuid(), convicted.getUuid(), date);
        if (matches.size() > 1) {
            throw new ConflictException("O histórico possui períodos de participação sobrepostos.");
        }
        return matches.isEmpty() ? null : matches.getFirst();
    }

    @Transactional
    public List<GroupParticipantMembership> getAndBackfill(Group group) {
        LocalDate legacyStart = group.getStartDate() != null
            ? group.getStartDate()
            : group.getCreatedAt() == null
                ? LocalDate.now(BUSINESS_ZONE)
                : group.getCreatedAt().atZone(BUSINESS_ZONE).toLocalDate();

        for (Convicted convicted : group.getConvicteds()) {
            if (repository.findByGroupIdAndConvictedIdAndEndDateIsNull(group.getId(), convicted.getId()).isEmpty()) {
                start(group, convicted, legacyStart);
            }
        }
        return repository.findAllByGroupUuid(group.getUuid());
    }
}
