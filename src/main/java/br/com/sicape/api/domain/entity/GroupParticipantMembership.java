package br.com.sicape.api.domain.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
    name = "reflection_group_participant_period",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_group_participant_period_start",
        columnNames = {"group_id", "convicted_id", "start_date"}
    ),
    indexes = @Index(
        name = "idx_group_participant_period_lookup",
        columnList = "group_id, convicted_id, start_date, end_date"
    )
)
public class GroupParticipantMembership extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, updatable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "convicted_id", nullable = false, updatable = false)
    private Convicted convicted;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    public GroupParticipantMembership(Group group, Convicted convicted, LocalDate startDate) {
        if (group == null || convicted == null || startDate == null) {
            throw new IllegalArgumentException("Grupo, apenado e início do vínculo são obrigatórios.");
        }
        this.group = group;
        this.convicted = convicted;
        this.startDate = startDate;
    }

    public void endOn(LocalDate date) {
        if (date == null || date.isBefore(startDate)) {
            throw new IllegalArgumentException("O fim do vínculo não pode ser anterior ao início.");
        }
        if (endDate == null || date.isBefore(endDate)) {
            endDate = date;
        }
    }

    public void rescheduleStartDate(LocalDate date) {
        if (date == null || endDate != null) {
            throw new IllegalArgumentException("Só é possível reagendar um vínculo ativo.");
        }
        startDate = date;
    }

    public boolean includes(LocalDate date) {
        return date != null && !date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate));
    }
}
