package br.com.sicape.api.domain.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import br.com.sicape.api.domain.enums.GroupMeetingStatus;
import br.com.sicape.api.domain.exception.ConflictException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "reflection_group_meeting",
    indexes = {
        @Index(name = "idx_group_meeting_group", columnList = "group_id"),
        @Index(name = "idx_group_meeting_group_date", columnList = "group_id, date")
    }
)
public class GroupMeeting extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, updatable = false)
    private Group group;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 500)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupMeetingStatus status = GroupMeetingStatus.PENDENTE;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<GroupMeetingPresence> presences = new ArrayList<>();

    public GroupMeeting(Group group, LocalDate date, String subject) {
        if (group == null) {
            throw new IllegalArgumentException("O grupo é obrigatório.");
        }
        if (date == null) {
            throw new IllegalArgumentException("A data do encontro é obrigatória.");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("O tema do encontro é obrigatório.");
        }
        this.group = group;
        this.date = date;
        this.subject = subject.trim();
        this.status = GroupMeetingStatus.PENDENTE;
    }

    public List<GroupMeetingPresence> getPresences() {
        return Collections.unmodifiableList(presences);
    }

    public boolean hasPresences() {
        return !presences.isEmpty();
    }

    public boolean updateSchedule(LocalDate date, String subject) {
        if (status != GroupMeetingStatus.PENDENTE) {
            throw new ConflictException(
                "status",
                "Apenas encontros pendentes podem ter data ou tema alterados."
            );
        }
        if (date == null) {
            throw new IllegalArgumentException("A data do encontro é obrigatória.");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("O tema do encontro é obrigatório.");
        }
        String normalizedSubject = subject.trim();
        if (Objects.equals(this.date, date) && Objects.equals(this.subject, normalizedSubject)) {
            return false;
        }
        this.date = date;
        this.subject = normalizedSubject;
        return true;
    }

    public void markRealized(List<GroupMeetingPresence> records) {
        if (status != GroupMeetingStatus.PENDENTE) {
            throw new ConflictException(
                "status",
                "Apenas encontros pendentes podem ser marcados como realizados."
            );
        }
        this.status = GroupMeetingStatus.REALIZADO;
        this.presences.clear();
        if (records != null) {
            this.presences.addAll(records);
        }
    }

    public void markNotRealized() {
        if (status == GroupMeetingStatus.CANCELADO) {
            return;
        }
        if (status == GroupMeetingStatus.REALIZADO || hasPresences()) {
            throw new ConflictException(
                "status",
                "Não é possível marcar como não realizado um encontro já realizado ou com presenças registradas."
            );
        }
        this.status = GroupMeetingStatus.CANCELADO;
    }
}
