package br.com.sicape.api.infrastructure.persistence.seeder;

import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.repository.JudicialDistrictRepository;
import br.com.sicape.api.domain.repository.UserRepository;
import br.com.sicape.api.domain.valueobject.Cpf;
import br.com.sicape.api.infrastructure.persistence.util.DevelopmentData;
import lombok.RequiredArgsConstructor;

/**
 * Seeder que cria um usuário de teste para desenvolvimento.
 */
@Component
@Order(2)
@Profile("development")
@RequiredArgsConstructor
public class UserSeeder implements CommandLineRunner {
    private final JudicialDistrictRepository districtRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    private final String defaultPassword = "123456789";

    @Override
    public void run(String... args) throws Exception {
        create(
            DevelopmentData.mockUuid(100),
            DevelopmentData.mockUuid(1),
            "Fulano da Silva",
            Cpf.of("51914372093"),
            UserRole.ADMIN
        );
        create(
            DevelopmentData.mockUuid(101),
            DevelopmentData.mockUuid(1),
            "Geralt de Rivia",
            Cpf.of("64282587067"),
            UserRole.OPERATOR
        );
        create(
            DevelopmentData.mockUuid(102),
            DevelopmentData.mockUuid(1),
            "David Bowie",
            Cpf.of("59982564099"),
            UserRole.OPERATOR
        );
    }

    private void create(UUID uuid, UUID districtUuid, String name, Cpf cpf, UserRole role)
    {
        if (userRepo.findByUuid(uuid).isPresent()) {
            return;
        }

        Optional<JudicialDistrict> optionalDistrict = districtRepo.findByUuid(districtUuid);

        if (optionalDistrict.isEmpty()) {
            throw new RuntimeException("Não foi possível encontrar o JudicialDistrict de mock");
        }

        User user = new User();
        user.setUuid(uuid);
        user.setCpf(cpf);
        user.setName(name);
        user.setPasswordHash(passwordEncoder.encode(defaultPassword));
        user.setDistrict(optionalDistrict.get());
        user.setRole(role);

        userRepo.save(user);
    }
}
