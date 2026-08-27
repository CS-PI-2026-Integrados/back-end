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
import br.com.sicape.api.application.user.dto.response.UserResponse;
import br.com.sicape.api.application.user.usecase.CreateUserUseCase;
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
            UserRole.OPERATOR
        );

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
            UserRole.OPERATOR
        );

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
            UserRole.OPERATOR
        );

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
            UserRole.OPERATOR
        );

        assertThatThrownBy(() -> createUserUseCase.execute(request, authContext))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Já existe um usuário cadastrado com este CPF.");
    }
}
