package br.com.sicape.api.infrastructure.persistence.seeder;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.JudicialProcess;
import br.com.sicape.api.domain.enums.ProcessStatus;
import br.com.sicape.api.domain.repository.JudicialDistrictRepository;
import br.com.sicape.api.domain.repository.JudicialProcessRepository;
import br.com.sicape.api.infrastructure.persistence.util.DevelopmentData;
import lombok.RequiredArgsConstructor;

@Component
@Order(3)
@Profile("development")
@RequiredArgsConstructor
public class JudicialProcessSeeder implements CommandLineRunner {
    private final JudicialDistrictRepository districtRepo;
    private final JudicialProcessRepository processRepo;

    @Override
    public void run(String... args) {
        UUID districtUuid = DevelopmentData.mockUuid(1);

        create(
            DevelopmentData.mockUuid(200),
            districtUuid,
            "0001234-56.2026.8.26.0001",
            ProcessStatus.ACTIVE
        );
        create(
            DevelopmentData.mockUuid(201),
            districtUuid,
            "0002345-67.2026.8.26.0001",
            ProcessStatus.ACTIVE
        );
        create(
            DevelopmentData.mockUuid(202),
            districtUuid,
            "0003456-78.2025.8.26.0001",
            ProcessStatus.INACTIVE
        );
    }

    private void create(UUID uuid, UUID districtUuid, String number, ProcessStatus status) {
        if (processRepo.findByUuid(uuid).isPresent()) {
            return;
        }

        JudicialDistrict district = districtRepo.findByUuid(districtUuid)
            .orElseThrow(() -> new IllegalStateException(
                "Não foi possível encontrar a comarca de desenvolvimento"
            ));

        JudicialProcess process = new JudicialProcess(number, status, district);
        process.setUuid(uuid);
        processRepo.save(process);
    }
}
