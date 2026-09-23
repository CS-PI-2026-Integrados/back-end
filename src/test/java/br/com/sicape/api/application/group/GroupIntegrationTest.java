package br.com.sicape.api.application.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import br.com.sicape.api.application.group.dto.request.CreateGroupRequest;
import br.com.sicape.api.application.group.dto.response.GroupResponse;
import br.com.sicape.api.application.group.usecase.CreateGroupUseCase;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.enums.GroupFrequency;
import br.com.sicape.api.domain.enums.GroupStatus;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.exception.ForbiddenException;
import br.com.sicape.api.domain.repository.GroupRepository;
import br.com.sicape.api.domain.repository.JudicialDistrictRepository;
import br.com.sicape.api.infrastructure.persistence.seeder.GroupSeeder;
import br.com.sicape.api.infrastructure.persistence.util.DevelopmentData;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:group-integration;MODE=MySQL;NON_KEYWORDS=USER,VALUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=chave-de-teste-com-pelo-menos-32-bytes",
    "jwt.issuer=sicape-api",
    "jwt.access-token-duration=15m"
})
@ActiveProfiles("development")
class GroupIntegrationTest {
    @Autowired
    private CreateGroupUseCase createGroupUseCase;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private JudicialDistrictRepository districtRepository;

    @Autowired
    private GroupSeeder groupSeeder;

    private JudicialDistrict district;

    @BeforeEach
    void setUp() {
        groupRepository.deleteAll();
        district = districtRepository.findByUuid(DevelopmentData.mockUuid(1))
            .orElseGet(() -> {
                JudicialDistrict created = new JudicialDistrict();
                created.setUuid(DevelopmentData.mockUuid(1));
                created.setName("Comarca de Teste");
                return districtRepository.save(created);
            });
    }

    @Test
    void shouldCreateGroupAsAdminAndOperatorWithoutConvicteds() {
        GroupResponse adminResponse = createGroupUseCase.execute(
            request("Grupo do Admin", List.of()),
            auth(UserRole.ADMIN)
        );
        GroupResponse operatorResponse = createGroupUseCase.execute(
            request("Grupo do Operador", null),
            auth(UserRole.OPERATOR)
        );

        assertThat(adminResponse.subject()).isEqualTo("Responsabilizacao");
        assertThat(adminResponse.presenters()).containsExactly("Ana Beatriz", "Carlos Eduardo");
        assertThat(adminResponse.status()).isEqualTo(GroupStatus.PLANNED);
        assertThat(adminResponse.convicteds()).isEmpty();
        assertThat(operatorResponse.status()).isEqualTo(GroupStatus.PLANNED);
        assertThat(operatorResponse.convicteds()).isEmpty();
        assertThat(groupRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldRejectGroupCreationWithoutAnAllowedRole() {
        assertThatThrownBy(() -> createGroupUseCase.execute(request("Sem autenticacao", List.of()), null))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("administradores e operadores");

        assertThatThrownBy(() -> createGroupUseCase.execute(request("Perfil invalido", List.of()), auth(null)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("administradores e operadores");
    }

    @Test
    void shouldSeedThreeDevelopmentGroupsOnlyOnce() throws Exception {
        groupSeeder.run();
        groupSeeder.run();

        assertThat(groupRepository.count()).isEqualTo(3);
        assertThat(groupRepository.findByUuid(DevelopmentData.mockUuid(400))).isPresent()
            .get()
            .extracting(group -> group.getStatus(), group -> group.getSubject())
            .containsExactly(GroupStatus.PLANNED, "Responsabilizacao");
        assertThat(groupRepository.findByUuid(DevelopmentData.mockUuid(402))).isPresent()
            .get()
            .extracting(group -> group.getPredictedEndDate())
            .isEqualTo(null);
    }

    private CreateGroupRequest request(String name, List<java.util.UUID> convictedUuids) {
        return new CreateGroupRequest(
            name,
            "Descricao do grupo",
            "Responsabilizacao",
            List.of("Ana Beatriz", "Carlos Eduardo"),
            8,
            6,
            "14:00",
            GroupFrequency.WEEKLY,
            LocalDate.of(2026, 9, 1),
            null,
            convictedUuids
        );
    }

    private AuthContext auth(UserRole role) {
        User user = new User();
        user.setRole(role);
        user.setDistrict(district);
        return new AuthContext(user, district, null);
    }
}
