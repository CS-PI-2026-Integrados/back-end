package br.com.sicape.api.domain.entity;

import java.time.Instant;

import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.valueobject.Cpf;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 11)
    private Cpf cpf;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private JudicialDistrict district;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role = UserRole.OPERATOR;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false)
    private boolean mustChangePassword = false;

    @Column
    private Instant lastAccessAt;

    @Column(length = 255)
    private String resetToken;

    @Column
    private Instant resetTokenExpiresAt;

    public User(
        String name,
        Cpf cpf,
        String email,
        String passwordHash,
        UserRole role,
        JudicialDistrict district
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome é obrigatório");
        }
        if (cpf == null) {
            throw new IllegalArgumentException("O CPF é obrigatório");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O e-mail é obrigatório");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("A senha é obrigatória");
        }
        if (role == null) {
            throw new IllegalArgumentException("O perfil de acesso é obrigatório");
        }
        if (district == null) {
            throw new IllegalArgumentException("A comarca é obrigatória");
        }

        this.name = name.trim();
        this.cpf = cpf;
        this.email = email.trim().toLowerCase();
        this.passwordHash = passwordHash;
        this.role = role;
        this.district = district;
        this.isActive = true;
        this.mustChangePassword = false;
    }
}
