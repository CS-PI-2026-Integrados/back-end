package br.com.sicape.api.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.application.user.dto.request.CreateUserRequest;
import br.com.sicape.api.application.user.dto.request.UpdateUserRequest;
import br.com.sicape.api.application.user.dto.response.UserResponse;
import br.com.sicape.api.application.user.usecase.CreateUserUseCase;
import br.com.sicape.api.application.user.usecase.DeleteUserUseCase;
import br.com.sicape.api.application.user.usecase.GetUserUseCase;
import br.com.sicape.api.application.user.usecase.ListUsersUseCase;
import br.com.sicape.api.application.user.usecase.UpdateUserUseCase;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.exception.ForbiddenException;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.repository.JudicialDistrictRepository;
import br.com.sicape.api.domain.repository.JudicialProcessRepository;
import br.com.sicape.api.domain.repository.SessionRepository;
import br.com.sicape.api.domain.repository.UserRepository;
import br.com.sicape.api.domain.valueobject.Cpf;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-integration;MODE=MySQL;NON_KEYWORDS=USER,VALUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=chave-de-teste-com-pelo-menos-32-bytes",
        "jwt.issuer=sicape-api",
        "jwt.access-token-duration=15m"
})
@ActiveProfiles("development")
class UserIntegrationTest {

    @Autowired
    private CreateUserUseCase createUserUseCase;

    @Autowired
    private ListUsersUseCase listUsersUseCase;

    @Autowired
    private GetUserUseCase getUserUseCase;

    @Autowired
    private UpdateUserUseCase updateUserUseCase;

    @Autowired
    private DeleteUserUseCase deleteUserUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JudicialDistrictRepository judicialDistrictRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ConvictedRepository convictedRepository;

    @Autowired
    private JudicialProcessRepository judicialProcessRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private JudicialDistrict district;
    private User adminUser;
    private User operatorUser;

    @BeforeEach
    void setUp() {
        clearDatabase();

        district = new JudicialDistrict();
        district.setName("Comarca de Teste");
        judicialDistrictRepository.save(district);

        adminUser = new User();
        adminUser.setName("Administrador");
        adminUser.setCpf(Cpf.of("51914372093"));
        adminUser.setEmail("admin@sicape.local");
        adminUser.setPasswordHash(passwordEncoder.encode("AdminPass123"));
        adminUser.setDistrict(district);
        adminUser.setRole(UserRole.ADMIN);
        userRepository.save(adminUser);

        operatorUser = new User();
        operatorUser.setName("Operador");
        operatorUser.setCpf(Cpf.of("64282587067"));
        operatorUser.setEmail("operator@sicape.local");
        operatorUser.setPasswordHash(passwordEncoder.encode("OperPass123"));
        operatorUser.setDistrict(district);
        operatorUser.setRole(UserRole.OPERATOR);
        userRepository.save(operatorUser);
    }

    @AfterEach
    void tearDown() {
        clearDatabase();
    }

    private void clearDatabase() {
        sessionRepository.deleteAll();
        convictedRepository.deleteAll();
        judicialProcessRepository.deleteAll();
        userRepository.deleteAll();
        judicialDistrictRepository.deleteAll();
    }

    @Test
    void shouldCreateUserSuccessfullyWhenAdmin() {
        AuthContext authContext = new AuthContext(adminUser, district, null);

        CreateUserRequest request = new CreateUserRequest(
                "Novo Operador",
                "59982564099",
                "novo.operador@sicape.local",
                "SenhaValida123",
                UserRole.OPERATOR);

        UserResponse response = createUserUseCase.execute(request, authContext);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Novo Operador");
        assertThat(response.email()).isEqualTo("novo.operador@sicape.local");
        assertThat(response.cpf()).isEqualTo("59982564099");
        assertThat(response.role()).isEqualTo(UserRole.OPERATOR);
        assertThat(response.isActive()).isTrue();

        User saved = userRepository.findByEmail("novo.operador@sicape.local").orElseThrow();
        assertThat(passwordEncoder.matches("SenhaValida123", saved.getPasswordHash())).isTrue();
    }

    @Test
    void shouldRejectCreationWhenUserIsNotAdmin() {
        AuthContext authContext = new AuthContext(operatorUser, district, null);

        CreateUserRequest request = new CreateUserRequest(
                "Outro Operador",
                "59982564099",
                "outro.operador@sicape.local",
                "SenhaValida123",
                UserRole.OPERATOR);

        assertThatThrownBy(() -> createUserUseCase.execute(request, authContext))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Apenas administradores");
    }

    @Test
    void shouldRejectCreationWithDuplicateEmail() {
        AuthContext authContext = new AuthContext(adminUser, district, null);

        CreateUserRequest request = new CreateUserRequest(
                "Duplicado Email",
                "59982564099",
                "admin@sicape.local",
                "SenhaValida123",
                UserRole.OPERATOR);

        assertThatThrownBy(() -> createUserUseCase.execute(request, authContext))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Já existe um usuário cadastrado com este e-mail.");
    }

    @Test
    void shouldRejectCreationWithDuplicateCpf() {
        AuthContext authContext = new AuthContext(adminUser, district, null);

        CreateUserRequest request = new CreateUserRequest(
                "Duplicado CPF",
                "51914372093",
                "outro@sicape.local",
                "SenhaValida123",
                UserRole.OPERATOR);

        assertThatThrownBy(() -> createUserUseCase.execute(request, authContext))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Já existe um usuário cadastrado com este CPF.");
    }

    @Test
    void shouldListUsersWithPaginationAndFilter() {
        AuthContext authContext = new AuthContext(adminUser, district, null);

        var response = listUsersUseCase.execute("admin", 0, 10, authContext);

        assertThat(response).isNotNull();
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().get(0).name()).isEqualTo("Administrador");

        var responseOper = listUsersUseCase.execute("oper", 0, 10, authContext);
        assertThat(responseOper.totalElements()).isEqualTo(1);
        assertThat(responseOper.content().get(0).name()).isEqualTo("Operador");

        var responseAll = listUsersUseCase.execute(null, 0, 10, authContext);
        assertThat(responseAll.totalElements()).isEqualTo(2);
    }

    @Test
    void shouldRejectListWhenNotAdmin() {
        AuthContext authContext = new AuthContext(operatorUser, district, null);

        assertThatThrownBy(() -> listUsersUseCase.execute(null, 0, 10, authContext))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldGetUserById() {
        AuthContext authContext = new AuthContext(adminUser, district, null);

        var response = getUserUseCase.execute(operatorUser.getUuid(), authContext);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Operador");
        assertThat(response.email()).isEqualTo("operator@sicape.local");
    }

    @Test
    void shouldRejectGetWhenNotAdmin() {
        AuthContext authContext = new AuthContext(operatorUser, district, null);

        assertThatThrownBy(() -> getUserUseCase.execute(adminUser.getUuid(), authContext))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        AuthContext authContext = new AuthContext(adminUser, district, null);

        assertThatThrownBy(() -> getUserUseCase.execute(java.util.UUID.randomUUID(), authContext))
                .isInstanceOf(br.com.sicape.api.domain.exception.ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        AuthContext authContext = new AuthContext(adminUser, district, null);
        UpdateUserRequest request = new UpdateUserRequest("Operador Editado", "editado@sicape.local", null,
                UserRole.ADMIN);

        UserResponse response = updateUserUseCase.execute(operatorUser.getUuid(), request, authContext);

        assertThat(response.name()).isEqualTo("Operador Editado");
        assertThat(response.email()).isEqualTo("editado@sicape.local");
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);

        User saved = userRepository.findByEmail("editado@sicape.local").orElseThrow();
        assertThat(saved.getPasswordHash()).isEqualTo(operatorUser.getPasswordHash());
    }

    @Test
    void shouldUpdateUserSuccessfullyWithPassword() {
        AuthContext authContext = new AuthContext(adminUser, district, null);
        UpdateUserRequest request = new UpdateUserRequest("Operador Editado", "operator@sicape.local", "NovaSenha123",
                UserRole.OPERATOR);

        String oldHash = operatorUser.getPasswordHash();
        UserResponse response = updateUserUseCase.execute(operatorUser.getUuid(), request, authContext);

        User saved = userRepository.findByEmail("operator@sicape.local").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo(oldHash);
        assertThat(passwordEncoder.matches("NovaSenha123", saved.getPasswordHash())).isTrue();
    }

    @Test
    void shouldRejectUpdateWhenNotAdmin() {
        AuthContext authContext = new AuthContext(operatorUser, district, null);
        UpdateUserRequest request = new UpdateUserRequest("Nome", "email@sicape.local", null, UserRole.OPERATOR);

        assertThatThrownBy(() -> updateUserUseCase.execute(adminUser.getUuid(), request, authContext))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldRejectUpdateWithDuplicateEmail() {
        AuthContext authContext = new AuthContext(adminUser, district, null);

        UpdateUserRequest request = new UpdateUserRequest("Nome", adminUser.getEmail(), null, UserRole.OPERATOR);

        assertThatThrownBy(() -> updateUserUseCase.execute(operatorUser.getUuid(), request, authContext))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Já existe um usuário cadastrado com este e-mail.");
    }

    @Test
    void shouldRejectUpdateOfOwnRole() {
        AuthContext authContext = new AuthContext(adminUser, district, null);

        UpdateUserRequest request = new UpdateUserRequest("Admin", adminUser.getEmail(), null, UserRole.OPERATOR);

        assertThatThrownBy(() -> updateUserUseCase.execute(adminUser.getUuid(), request, authContext))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Você não pode alterar seu próprio nível de acesso.");
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingNonExistentUser() {
        AuthContext authContext = new AuthContext(adminUser, district, null);
        UpdateUserRequest request = new UpdateUserRequest("Nome", "email@sicape.local", null, UserRole.OPERATOR);

        assertThatThrownBy(() -> updateUserUseCase.execute(java.util.UUID.randomUUID(), request, authContext))
                .isInstanceOf(br.com.sicape.api.domain.exception.ResourceNotFoundException.class);
    }

    @Test
    void shouldSoftDeleteUserSuccessfully() {
        AuthContext authContext = new AuthContext(adminUser, district, null);
        
        deleteUserUseCase.execute(operatorUser.getUuid(), authContext);
        
        User saved = userRepository.findByEmail("operator@sicape.local").orElseThrow();
        assertThat(saved.isActive()).isFalse();
    }

    @Test
    void shouldRejectSoftDeleteWhenNotAdmin() {
        AuthContext authContext = new AuthContext(operatorUser, district, null);

        assertThatThrownBy(() -> deleteUserUseCase.execute(adminUser.getUuid(), authContext))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Apenas administradores podem remover usuários.");
    }

    @Test
    void shouldRejectSoftDeleteOfOwnUser() {
        AuthContext authContext = new AuthContext(adminUser, district, null);

        assertThatThrownBy(() -> deleteUserUseCase.execute(adminUser.getUuid(), authContext))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Você não pode remover seu próprio usuário.");
    }

    @Test
    void shouldThrowNotFoundWhenDeletingNonExistentUser() {
        AuthContext authContext = new AuthContext(adminUser, district, null);

        assertThatThrownBy(() -> deleteUserUseCase.execute(java.util.UUID.randomUUID(), authContext))
            .isInstanceOf(br.com.sicape.api.domain.exception.ResourceNotFoundException.class);
    }
}
