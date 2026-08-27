package br.com.sicape.api.infrastructure.persistence.seeder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.ConvictedProcess;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.JudicialProcess;
import br.com.sicape.api.domain.enums.EmploymentStatus;
import br.com.sicape.api.domain.provider.PhotoUrlProvider;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.repository.JudicialDistrictRepository;
import br.com.sicape.api.domain.repository.JudicialProcessRepository;
import br.com.sicape.api.domain.valueobject.Address;
import br.com.sicape.api.domain.valueobject.Cpf;
import br.com.sicape.api.infrastructure.persistence.util.DevelopmentData;
import lombok.RequiredArgsConstructor;

@Component
@Order(4)
@Profile("development")
@RequiredArgsConstructor
public class ConvictedSeeder implements CommandLineRunner {
    private final JudicialDistrictRepository districtRepo;
    private final JudicialProcessRepository processRepo;
    private final ConvictedRepository convictedRepo;
    private final PhotoUrlProvider photoUrlProvider;

    @Override
    public void run(String... args) {
        UUID districtUuid = DevelopmentData.mockUuid(1);

        createArthurMorgan(districtUuid);
        createSarahConnor(districtUuid);
        createThomasAnderson(districtUuid);
    }

    private void createArthurMorgan(UUID districtUuid) {
        UUID uuid = DevelopmentData.mockUuid(300);
        if (convictedRepo.findByUuid(uuid).isPresent()) {
            return;
        }

        Convicted convicted = new Convicted(
            "Arthur Morgan",
            Cpf.of("52998224725"),
            LocalDate.of(1980, 6, 15),
            "(11) 98888-1001",
            new Address(
                "01001000",
                "Praça da Sé",
                "100",
                "Apto 12",
                "Sé",
                "São Paulo",
                "SP"
            ),
            EmploymentStatus.INFORMAL_WORK,
            getDistrict(districtUuid)
        );
        convicted.setUuid(uuid);
        convicted.completePhoto(photoUrlProvider.provide(uuid));
        convicted.replaceProcesses(List.of(
            new ConvictedProcess(convicted, getProcess(200), true),
            new ConvictedProcess(convicted, getProcess(202), false)
        ));

        convictedRepo.save(convicted);
    }

    private void createSarahConnor(UUID districtUuid) {
        UUID uuid = DevelopmentData.mockUuid(301);
        if (convictedRepo.findByUuid(uuid).isPresent()) {
            return;
        }

        Convicted convicted = new Convicted(
            "Sarah Connor",
            Cpf.of("11144477735"),
            LocalDate.of(1985, 5, 13),
            "(11) 98888-1002",
            new Address(
                "01310100",
                "Avenida Paulista",
                "1578",
                null,
                "Bela Vista",
                "São Paulo",
                "SP"
            ),
            EmploymentStatus.FORMAL_WORK,
            getDistrict(districtUuid)
        );
        convicted.setUuid(uuid);
        convicted.completePhoto(photoUrlProvider.provide(uuid));
        convicted.replaceProcesses(List.of(
            new ConvictedProcess(convicted, getProcess(200), true),
            new ConvictedProcess(convicted, getProcess(201), false)
        ));

        convictedRepo.save(convicted);
    }

    private void createThomasAnderson(UUID districtUuid) {
        UUID uuid = DevelopmentData.mockUuid(302);
        if (convictedRepo.findByUuid(uuid).isPresent()) {
            return;
        }

        Convicted convicted = new Convicted(
            "Thomas Anderson",
            Cpf.of("12345678909"),
            LocalDate.of(1990, 3, 11),
            "(11) 98888-1003",
            new Address(
                "04538133",
                "Rua das Flores",
                "42",
                "Casa 2",
                "Itaim Bibi",
                "São Paulo",
                "SP"
            ),
            EmploymentStatus.UNEMPLOYED,
            getDistrict(districtUuid)
        );
        convicted.setUuid(uuid);
        convicted.replaceProcesses(List.of(
            new ConvictedProcess(convicted, getProcess(202), true)
        ));

        convictedRepo.save(convicted);
    }

    private JudicialDistrict getDistrict(UUID uuid) {
        return districtRepo.findByUuid(uuid)
            .orElseThrow(() -> new IllegalStateException(
                "Não foi possível encontrar a comarca de desenvolvimento"
            ));
    }

    private JudicialProcess getProcess(int uuidValue) {
        return processRepo.findByUuid(DevelopmentData.mockUuid(uuidValue))
            .orElseThrow(() -> new IllegalStateException(
                "Não foi possível encontrar o processo de desenvolvimento"
            ));
    }
}
