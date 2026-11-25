package br.com.sicape.api.domain.entity;

import br.com.sicape.api.domain.enums.GroupJustificationType;
import br.com.sicape.api.domain.enums.GroupPresenceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "reflection_group_meeting_attendance",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_group_meeting_attendance_convicted",
        columnNames = {"meeting_id", "convicted_id"}
    ),
    indexes = {
        @Index(name = "idx_group_meeting_attendance_meeting", columnList = "meeting_id"),
        @Index(name = "idx_group_meeting_attendance_convicted", columnList = "convicted_id")
    }
)
public class GroupMeetingPresence extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false, updatable = false)
    private GroupMeeting meeting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "convicted_id", nullable = false, updatable = false)
    private Convicted convicted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id")
    private GroupParticipantMembership membership;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupPresenceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "justification_type", length = 30)
    private GroupJustificationType justificationType;

    @Column(name = "justification_text", length = 200)
    private String justificationText;

    public GroupMeetingPresence(
        GroupMeeting meeting,
        Convicted convicted,
        GroupParticipantMembership membership,
        GroupPresenceStatus status,
        GroupJustificationType justificationType,
        String justificationText
    ) {
        if (meeting == null) {
            throw new IllegalArgumentException("O encontro é obrigatório.");
        }
        if (convicted == null) {
            throw new IllegalArgumentException("O apenado é obrigatório.");
        }
        if (status == null) {
            throw new IllegalArgumentException("O status da presença é obrigatório.");
        }
        String normalizedText = justificationText == null || justificationText.isBlank()
            ? null
            : justificationText.trim();
        if (normalizedText != null && normalizedText.length() > 200) {
            throw new IllegalArgumentException("A observação da justificação deve ter no máximo 200 caracteres.");
        }
        if (status == GroupPresenceStatus.JUSTIFICADO) {
            if (justificationType == null) {
                throw new IllegalArgumentException("O tipo de justificação é obrigatório quando o status for JUSTIFICADO.");
            }
        } else {
            if (justificationType != null || normalizedText != null) {
                throw new IllegalArgumentException("Justificativa só pode ser informada quando o status for JUSTIFICADO.");
            }
        }

        this.meeting = meeting;
        this.convicted = convicted;
        this.membership = membership;
        this.status = status;
        this.justificationType = justificationType;
        this.justificationText = normalizedText;
    }
}
