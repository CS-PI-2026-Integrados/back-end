package br.com.sicape.api.domain.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import br.com.sicape.api.domain.enums.ConvictedStatus;
import br.com.sicape.api.domain.enums.EmploymentStatus;
import br.com.sicape.api.domain.enums.MediaAssetKind;
import br.com.sicape.api.domain.enums.ProcessStatus;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.valueobject.Address;
import br.com.sicape.api.domain.valueobject.Cpf;
import br.com.sicape.api.domain.valueobject.Phone;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Convicted extends BaseEntity {
    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 11)
    private Cpf cpf;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Embedded
    private Phone phone;

    @Embedded
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EmploymentStatus employmentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConvictedStatus status = ConvictedStatus.ACTIVE;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_asset_id", unique = true)
    private MediaAsset photo;

    private Instant deactivatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private User deactivatedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private JudicialDistrict district;

    @OneToMany(mappedBy = "convicted", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<ConvictedProcess> processes = new ArrayList<>();

    public Convicted(
        String name,
        Cpf cpf,
        LocalDate birthDate,
        Phone phone,
        Address address,
        EmploymentStatus employmentStatus,
        JudicialDistrict district
    ) {
        updateName(name);
        this.cpf = cpf;
        updateBirthDate(birthDate);
        updatePhone(phone);
        this.address = address;
        this.employmentStatus = employmentStatus;
        this.district = district;
    }

    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome é obrigatório");
        }
        this.name = name.trim();
    }

    public void updateCpf(Cpf cpf) {
        if (cpf == null) {
            throw new IllegalArgumentException("O CPF é obrigatório");
        }
        this.cpf = cpf;
    }

    public void updateBirthDate(LocalDate birthDate) {
        if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("A data de nascimento não pode ser futura");
        }
        this.birthDate = birthDate;
    }

    public void updatePhone(Phone phone) {
        if (phone == null) {
            throw new IllegalArgumentException("O telefone é obrigatório");
        }
        this.phone = phone;
    }

    public void updateAddress(Address address) {
        if (address == null) {
            throw new IllegalArgumentException("O endereço é obrigatório");
        }
        this.address = address;
    }

    public void updateEmploymentStatus(EmploymentStatus employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public void replaceProcesses(Collection<ConvictedProcess> newProcesses) {
        if (!newProcesses.isEmpty()
            && newProcesses.stream().filter(ConvictedProcess::isPrincipal).count() != 1) {
            throw new IllegalArgumentException("Informe exatamente um processo principal");
        }
        this.processes.clear();
        this.processes.addAll(newProcesses);
    }

    public boolean hasActiveProcess() {
        return processes.stream().anyMatch(link -> link.getProcess().getStatus() == ProcessStatus.ACTIVE);
    }

    public void completePhoto(MediaAsset photo) {
        if (status == ConvictedStatus.INACTIVE) {
            throw new IllegalStateException("Não é possível alterar a foto de um condenado inativo");
        }
        if (photo == null || photo.getKind() != MediaAssetKind.PHOTO) {
            throw new IllegalArgumentException("A foto deve ser uma mídia do tipo foto");
        }
        this.photo = photo;
    }

    public void remove(User user) {
        if (hasActiveProcess()) {
            throw new ConflictException("Não é possível remover um condenado vinculado a processo ativo.");
        }
        this.status = ConvictedStatus.INACTIVE;
        this.deactivatedAt = Instant.now();
        this.deactivatedBy = user;
    }
}
