package br.com.sicape.api.application.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import br.com.sicape.api.application.group.dto.request.CreateGroupRequest;
import br.com.sicape.api.application.group.dto.request.UpdateGroupRequest;
import br.com.sicape.api.application.group.dto.response.GroupResponse;
import br.com.sicape.api.application.group.usecase.CreateGroupUseCase;
import br.com.sicape.api.application.group.usecase.DeleteGroupUseCase;
import br.com.sicape.api.application.group.usecase.GetGroupUseCase;
import br.com.sicape.api.application.group.usecase.ListGroupUseCase;
import br.com.sicape.api.application.group.usecase.UpdateGroupUseCase;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.enums.GroupFrequency;
import br.com.sicape.api.domain.enums.GroupStatus;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.exception.ForbiddenException;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.repository.GroupRepository;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.repository.JudicialDistrictRepository;
import br.com.sicape.api.domain.valueobject.Address;
import br.com.sicape.api.domain.valueobject.Cpf;
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
    private GetGroupUseCase getGroupUseCase;

    @Autowired
    private DeleteGroupUseCase deleteGroupUseCase;

    @Autowired
    private ListGroupUseCase listGroupUseCase;

    @Autowired
    private UpdateGroupUseCase updateGroupUseCase;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ConvictedRepository convictedRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JudicialDistrictRepository districtRepository;

    @Autowired
    private GroupSeeder groupSeeder;

    private JudicialDistrict district;

    @BeforeEach
    void setUp() {
        groupRepository.deleteAll();
        convictedRepository.deleteAll();
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
    void shouldGetGroupWithPresentersAndConvicteds() {
        GroupResponse created = createGroupUseCase.execute(
            request("Grupo para consulta", List.of()),
            auth(UserRole.ADMIN)
        );

        GroupResponse found = getGroupUseCase.execute(created.uuid(), auth(UserRole.ADMIN));

        assertThat(found.presenters()).containsExactly("Ana Beatriz", "Carlos Eduardo");
        assertThat(found.convicteds()).isEmpty();
    }

    @Test
    void shouldSoftDeleteGroupHideItFromListAndPreserveConvictedLink() {
        Convicted convicted = convictedRepository.save(new Convicted(
            "Apenado de teste",
            Cpf.of("51914372093"),
            LocalDate.of(1990, 1, 1),
            "11999999999",
            new Address("01001000", "Praca da Se", "1", null, "Se", "Sao Paulo", "SP"),
            null,
            district
        ));
        GroupResponse created = createGroupUseCase.execute(
            request("Grupo removido", List.of(convicted.getUuid())),
            auth(UserRole.ADMIN)
        );
        Long groupId = groupRepository.findByUuid(created.uuid()).orElseThrow().getId();

        deleteGroupUseCase.execute(created.uuid(), auth(UserRole.OPERATOR));

        assertThat(groupRepository.findByUuid(created.uuid()).orElseThrow().isDeleted()).isTrue();
        assertThat(listGroupUseCase.execute(null, null, null, 0, 20, auth(UserRole.ADMIN)).content())
            .noneMatch(group -> group.uuid().equals(created.uuid()));
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from reflection_group_convicted where group_id = ?",
            Integer.class,
            groupId
        )).isEqualTo(1);
        assertThatThrownBy(() -> getGroupUseCase.execute(created.uuid(), auth(UserRole.ADMIN)))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingGroup() {
        assertThatThrownBy(() -> deleteGroupUseCase.execute(DevelopmentData.mockUuid(999), auth(UserRole.ADMIN)))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldRejectGroupDeletionWithoutAllowedRole() {
        assertThatThrownBy(() -> deleteGroupUseCase.execute(DevelopmentData.mockUuid(999), null))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldUpdateEditableGroupFieldsAndReplacePresenters() {
        GroupResponse created = createGroupUseCase.execute(
            request("Grupo para atualizar", List.of()),
            auth(UserRole.ADMIN)
        );
        UpdateGroupRequest update = updateRequest("Grupo atualizado", List.of("Marina Costa"), GroupStatus.ACTIVE);
        update.setPredictedEndDate(LocalDate.of(2026, 10, 15));
        update.setStartDate(LocalDate.of(2026, 9, 10));

        GroupResponse updated = updateGroupUseCase.execute(created.uuid(), update, auth(UserRole.OPERATOR));

        assertThat(updated.name()).isEqualTo("Grupo atualizado");
        assertThat(updated.subject()).isEqualTo("Responsabilizacao");
        assertThat(updated.presenters()).containsExactly("Marina Costa");
        assertThat(updated.predictedEndDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(updated.startDate()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(updated.status()).isEqualTo(GroupStatus.ACTIVE);
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
        GroupResponse created = createGroupUseCase.execute(
            request("Grupo parcial", List.of()),
            auth(UserRole.ADMIN)
        );
        UpdateGroupRequest update = new UpdateGroupRequest();
        update.setName("Somente o nome mudou");

        GroupResponse updated = updateGroupUseCase.execute(created.uuid(), update, auth(UserRole.OPERATOR));

        assertThat(updated.name()).isEqualTo("Somente o nome mudou");
        assertThat(updated.subject()).isEqualTo(created.subject());
        assertThat(updated.presenters()).isEqualTo(created.presenters());
        assertThat(updated.status()).isEqualTo(created.status());
        assertThat(updated.startDate()).isEqualTo(created.startDate());
    }

    @Test
    void shouldRejectUpdateWithoutAnyFields() {
        GroupResponse created = createGroupUseCase.execute(
            request("Grupo sem alteracoes", List.of()),
            auth(UserRole.ADMIN)
        );

        assertThatThrownBy(() -> updateGroupUseCase.execute(
            created.uuid(), new UpdateGroupRequest(), auth(UserRole.ADMIN)
        )).isInstanceOf(ValidationException.class)
            .satisfies(exception -> assertThat(((ValidationException) exception).getFields())
                .anyMatch(field -> field.field().equals("request")));
    }

    @Test
    void shouldRejectUpdateWithoutPresenters() {
        GroupResponse created = createGroupUseCase.execute(
            request("Grupo sem ministrante", List.of()),
            auth(UserRole.ADMIN)
        );
        UpdateGroupRequest update = updateRequest("Grupo sem ministrante", List.of(), GroupStatus.PLANNED);

        assertThatThrownBy(() -> updateGroupUseCase.execute(created.uuid(), update, auth(UserRole.ADMIN)))
            .isInstanceOf(ValidationException.class)
            .satisfies(exception -> assertThat(((ValidationException) exception).getFields())
                .anyMatch(field -> field.field().equals("presenters")));
    }

    @Test
    void shouldRejectFieldsOutsideUpdateContract() {
        GroupResponse created = createGroupUseCase.execute(
            request("Grupo com campo proibido", List.of()),
            auth(UserRole.ADMIN)
        );
        UpdateGroupRequest update = updateRequest("Grupo com campo proibido", List.of("Ana"), GroupStatus.PLANNED);
        update.addUnsupportedField("convicteds", List.of());

        assertThatThrownBy(() -> updateGroupUseCase.execute(created.uuid(), update, auth(UserRole.ADMIN)))
            .isInstanceOf(ValidationException.class)
            .satisfies(exception -> assertThat(((ValidationException) exception).getFields())
                .anyMatch(field -> field.field().equals("convicteds")));
    }

    @Test
    void shouldCaptureUnsupportedJsonFieldsAndBindConditionalStartDate() throws Exception {
        UpdateGroupRequest update = objectMapper.readValue("""
            {
              "name": "Grupo",
              "subject": "Tema",
              "presenters": ["Ana"],
              "status": "PLANNED",
              "start_date": "2026-09-10",
              "convicteds": []
            }
            """, UpdateGroupRequest.class);

        assertThat(update.isStartDateProvided()).isTrue();
        assertThat(update.getStartDate()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(update.getUnsupportedFields()).containsExactly("convicteds");

        UpdateGroupRequest partialUpdate = objectMapper.readValue("{\"name\":\"Novo nome\"}", UpdateGroupRequest.class);
        assertThat(partialUpdate.isNameProvided()).isTrue();
        assertThat(partialUpdate.isSubjectProvided()).isFalse();
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingGroup() {
        assertThatThrownBy(() -> updateGroupUseCase.execute(
            DevelopmentData.mockUuid(999),
            updateRequest("Grupo inexistente", List.of("Ana"), GroupStatus.PLANNED),
            auth(UserRole.ADMIN)
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldRejectUpdateWithoutAllowedRole() {
        UpdateGroupRequest update = updateRequest("Sem permissao", List.of("Ana"), GroupStatus.PLANNED);

        assertThatThrownBy(() -> updateGroupUseCase.execute(DevelopmentData.mockUuid(999), update, null))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldRejectChangingStartDateAfterGroupStarts() {
        GroupResponse created = createGroupUseCase.execute(
            request("Grupo iniciado", List.of()),
            auth(UserRole.ADMIN)
        );
        var group = groupRepository.findByUuidAndDistrict(created.uuid(), district).orElseThrow();
        group.setStatus(GroupStatus.ACTIVE);
        groupRepository.save(group);

        UpdateGroupRequest update = updateRequest("Grupo iniciado", List.of("Ana"), GroupStatus.ACTIVE);
        update.setStartDate(LocalDate.of(2026, 9, 10));

        assertThatThrownBy(() -> updateGroupUseCase.execute(created.uuid(), update, auth(UserRole.ADMIN)))
            .isInstanceOf(ValidationException.class)
            .satisfies(exception -> assertThat(((ValidationException) exception).getFields())
                .anyMatch(field -> field.field().equals("start_date")));
    }

    @Test
    void shouldRejectPredictedEndDateBeforeCurrentStartDate() {
        GroupResponse created = createGroupUseCase.execute(
            request("Grupo com data inválida", List.of()),
            auth(UserRole.ADMIN)
        );
        UpdateGroupRequest update = updateRequest("Grupo com data inválida", List.of("Ana"), GroupStatus.PLANNED);
        update.setPredictedEndDate(LocalDate.of(2026, 8, 31));

        assertThatThrownBy(() -> updateGroupUseCase.execute(created.uuid(), update, auth(UserRole.ADMIN)))
            .isInstanceOf(ValidationException.class)
            .satisfies(exception -> assertThat(((ValidationException) exception).getFields())
                .anyMatch(field -> field.field().equals("predicted_end_date")));
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

    private UpdateGroupRequest updateRequest(String name, List<String> presenters, GroupStatus status) {
        UpdateGroupRequest request = new UpdateGroupRequest();
        request.setName(name);
        request.setSubject("Responsabilizacao");
        request.setPresenters(presenters);
        request.setStatus(status);
        return request;
    }

    private AuthContext auth(UserRole role) {
        User user = new User();
        user.setRole(role);
        user.setDistrict(district);
        return new AuthContext(user, district, null);
    }
}
