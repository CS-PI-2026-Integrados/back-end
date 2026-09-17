package br.com.sicape.api.domain.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import br.com.sicape.api.domain.enums.GroupFrequency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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

    @Column
    private Integer minimumMeetingsCount;

    @Column
    private Integer totalMeetingsCounts;

    @Column
    @Enumerated
    private GroupFrequency frequency;

    @Column
    private LocalTime meetingBaseTime;

    // @OneToMany(mappedBy = "group")
    // private List<Convicted> convicteds;

    @Column(nullable = true)
    private LocalDate startDate;

    @Column(nullable = true)
    private LocalDate predictedEndDate;

    @Column(nullable = true)
    private LocalDate realEndDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private JudicialDistrict district;
}
