package br.com.sicape.api.infrastructure.persistence.seeder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import br.com.sicape.api.domain.entity.Group;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.enums.GroupFrequency;
import br.com.sicape.api.domain.enums.GroupStatus;
import br.com.sicape.api.domain.repository.GroupRepository;
import br.com.sicape.api.domain.repository.JudicialDistrictRepository;
import br.com.sicape.api.infrastructure.persistence.util.DevelopmentData;
import lombok.RequiredArgsConstructor;

@Component
@Order(5)
@Profile("development")
@RequiredArgsConstructor
public class GroupSeeder implements CommandLineRunner {
    private final GroupRepository groupRepo;
    private final JudicialDistrictRepository districtRepo;

    @Override
    public void run(String... args) {
        UUID districtUuid = DevelopmentData.mockUuid(1);

        create(
            DevelopmentData.mockUuid(400),
            districtUuid,
            "Grupo de Responsabilizacao - Manha",
            "Encontros para reflexao sobre responsabilizacao e convivencia.",
            "Responsabilizacao",
            List.of("Ana Beatriz", "Carlos Eduardo"),
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 11, 24)
        );
        create(
            DevelopmentData.mockUuid(401),
            districtUuid,
            "Grupo de Comunicacao Nao Violenta",
            "Encontros para praticar comunicacao respeitosa e resolucao de conflitos.",
            "Comunicacao nao violenta",
            List.of("Mariana Souza", "Rafael Lima"),
            LocalDate.of(2026, 10, 7),
            LocalDate.of(2026, 12, 16)
        );
        create(
            DevelopmentData.mockUuid(402),
            districtUuid,
            "Grupo de Projeto de Vida",
            "Encontros para construcao de metas pessoais e profissionais.",
            "Projeto de vida",
            List.of("Fernanda Alves", "Joao Pedro"),
            LocalDate.of(2026, 11, 4),
            null
        );
    }

    private void create(
        UUID uuid,
        UUID districtUuid,
        String name,
        String description,
        String subject,
        List<String> presenters,
        LocalDate startDate,
        LocalDate predictedEndDate
    ) {
        if (groupRepo.findByUuid(uuid).isPresent()) {
            return;
        }

        JudicialDistrict district = districtRepo.findByUuid(districtUuid)
            .orElseThrow(() -> new IllegalStateException(
                "Nao foi possivel encontrar a comarca de desenvolvimento"
            ));

        Group group = new Group();
        group.setUuid(uuid);
        group.setName(name);
        group.setDescription(description);
        group.setSubject(subject);
        group.setPresenters(presenters);
        group.setStatus(GroupStatus.PLANNED);
        group.setFrequency(GroupFrequency.WEEKLY);
        group.setMeetingBaseTime(java.time.LocalTime.of(14, 0));
        group.setMinimumMeetingsCount(6);
        group.setTotalMeetingsCounts(8);
        group.setStartDate(startDate);
        group.setPredictedEndDate(predictedEndDate);
        group.setDistrict(district);

        groupRepo.save(group);
    }
}
