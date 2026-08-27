package br.com.sicape.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import br.com.sicape.api.domain.enums.ProcessStatus;

@Entity
@Getter
@Table(name = "judicial_process")
@BatchSize(size = 50)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JudicialProcess extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String number;

    @Column(nullable = false, length = 50)
    private String normalizedNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private JudicialDistrict district;

    public JudicialProcess(String number, ProcessStatus status, JudicialDistrict district) {
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException("O número do processo é obrigatório");
        }
        this.number = number.trim();
        this.normalizedNumber = number.replaceAll("\\D", "");
        this.status = status;
        this.district = district;
    }
}
