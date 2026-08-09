package br.com.sicape.api.infrastructure.persistence.seeder;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.repository.JudicialDistrictRepository;
import br.com.sicape.api.infrastructure.persistence.util.DevelopmentData;
import lombok.RequiredArgsConstructor;

@Component
@Order(1)
@Profile("development")
@RequiredArgsConstructor
public class JudicialDistrictSeeder implements CommandLineRunner {
    private final JudicialDistrictRepository repo;

    @Override
    public void run(String... args) throws Exception {
        create(DevelopmentData.mockUuid(1), "Comarca de New Vegas");
        create(DevelopmentData.mockUuid(2), "Comarca de Silent Hill");
        create(DevelopmentData.mockUuid(3), "Comarca de Raccoon City");
    }

    private void create(UUID uuid, String name)
    {
        if (repo.findByUuid(uuid).isPresent()) {
            return;
        }

        JudicialDistrict district = new JudicialDistrict();
        district.setUuid(uuid);
        district.setName(name);

        repo.save(district);
    }
}
