package br.com.sicape.api.application.group.dto.response;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.sicape.api.domain.entity.GroupMeeting;
import br.com.sicape.api.domain.enums.GroupMeetingStatus;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record GroupMeetingResponse(
        UUID uuid,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        String subject,
        GroupMeetingStatus status,
        Long version,
        List<GroupMeetingAttendanceResponse> attendance) {
    public static GroupMeetingResponse from(GroupMeeting meeting) {
        List<GroupMeetingAttendanceResponse> attendance = meeting.getPresences().stream()
                .sorted(Comparator
                        .comparing(
                                (br.com.sicape.api.domain.entity.GroupMeetingPresence p) -> p.getConvicted().getName(),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(
                                (br.com.sicape.api.domain.entity.GroupMeetingPresence p) -> p.getConvicted().getUuid()))
                .map(GroupMeetingAttendanceResponse::from)
                .toList();
        return new GroupMeetingResponse(
                meeting.getUuid(),
                meeting.getDate(),
                meeting.getSubject(),
                meeting.getStatus(),
                meeting.getVersion() == null ? 0L : meeting.getVersion(),
                attendance);
    }
}
