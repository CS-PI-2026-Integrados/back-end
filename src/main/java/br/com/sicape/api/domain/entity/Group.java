package br.com.sicape.api.domain.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import br.com.sicape.api.domain.enums.GroupFrequency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
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

    @OneToMany(mappedBy = "group")
    private List<Convicted> convicteds;

    @Column(nullable = true)
    private LocalDate startDate;

    @Column(nullable = true)
    private LocalDate predictedEndDate;

    @Column(nullable = true)
    private LocalDate realEndDate;
}
