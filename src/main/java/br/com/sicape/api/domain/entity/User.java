package br.com.sicape.api.domain.entity;

import br.com.sicape.api.domain.valueobject.Cpf;
import br.com.sicape.api.domain.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class User extends BaseEntity {

    @Column(length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 11)
    private Cpf cpf;

    @Column(nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private JudicialDistrict district;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.OPERATOR;
}
