package br.com.sicape.api.application.group.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import br.com.sicape.api.domain.entity.GroupParticipantMembership;

public record GroupParticipantMembershipResponse(
    UUID uuid,
    UUID convictedUuid,
    String convictedName,
    LocalDate startDate,
    LocalDate endDate
) {
    public static GroupParticipantMembershipResponse from(GroupParticipantMembership membership) {
        return new GroupParticipantMembershipResponse(
            membership.getUuid(),
            membership.getConvicted().getUuid(),
            membership.getConvicted().getName(),
            membership.getStartDate(),
            membership.getEndDate()
        );
    }
}
