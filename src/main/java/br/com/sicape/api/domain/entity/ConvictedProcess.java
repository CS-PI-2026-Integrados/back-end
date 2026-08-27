package br.com.sicape.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
    name = "convicted_process",
    uniqueConstraints = @UniqueConstraint(columnNames = {"convicted_id", "process_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConvictedProcess extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "convicted_id", nullable = false)
    private Convicted convicted;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "process_id", nullable = false)
    private JudicialProcess process;

    @Column(nullable = false)
    private boolean principal;

    public ConvictedProcess(Convicted convicted, JudicialProcess process, boolean principal) {
        this.convicted = convicted;
        this.process = process;
        this.principal = principal;
    }
}
