package br.com.sicape.api.application.group.dto.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.sicape.api.domain.entity.GroupMeetingPresence;
import br.com.sicape.api.domain.enums.GroupJustificationType;
import br.com.sicape.api.domain.enums.GroupPresenceStatus;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record GroupMeetingAttendanceResponse(
    UUID convictedUuid,
    String convictedName,
    UUID membershipUuid,
    GroupPresenceStatus status,
    GroupJustificationType justificationType,
    String justificationText
) {
    public static GroupMeetingAttendanceResponse from(GroupMeetingPresence presence) {
        return new GroupMeetingAttendanceResponse(
            presence.getConvicted().getUuid(),
            presence.getConvicted().getName(),
            presence.getMembership() == null ? null : presence.getMembership().getUuid(),
            presence.getStatus(),
            presence.getJustificationType(),
            presence.getJustificationText()
        );
    }
}
