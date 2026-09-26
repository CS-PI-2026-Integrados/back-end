package br.com.sicape.api.domain.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import br.com.sicape.api.domain.enums.GroupFrequency;
import br.com.sicape.api.domain.enums.GroupStatus;
import br.com.sicape.api.domain.exception.ConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@NoArgsConstructor
@Table(name = "reflection_group")
public class Group extends BaseEntity {
    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 512)
    private String description;

    @Column(nullable = false, length = 255)
    private String subject;

    @ElementCollection
    @CollectionTable(
        name = "reflection_group_presenter",
        joinColumns = @JoinColumn(name = "group_id")
    )
    @Column(name = "presenter", nullable = false, length = 255)
    private List<String> presenters = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GroupStatus status = GroupStatus.PLANNED;

    @Column
    private Integer minimumMeetingsCount;

    @Column
    private Integer totalMeetingsCounts;

    @Column
    @Enumerated
    private GroupFrequency frequency;

    @Column
    private LocalTime meetingBaseTime;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "reflection_group_convicted",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "convicted_id")
    )
    private List<Convicted> convicteds = new ArrayList<>();

    @Column(nullable = true)
    private LocalDate startDate;

    @Column(nullable = true)
    private LocalDate predictedEndDate;

    @Column(nullable = true)
    private LocalDate realEndDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private JudicialDistrict district;

    public void addConvicted(Convicted convicted) {
        if (convicteds.contains(convicted)) {
            throw new ConflictException("O apenado já está vinculado ao grupo reflexivo.");
        }
        convicteds.add(convicted);
    }

    public void removeConvicted(Convicted convicted) {
        convicteds.remove(convicted);
    }
}
