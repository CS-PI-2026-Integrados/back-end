package br.com.sicape.api.application.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import br.com.sicape.api.application.oauth.login.CreateSessionRequest;
import br.com.sicape.api.application.oauth.login.CreateSessionResponse;
import br.com.sicape.api.application.oauth.login.CreateSessionUseCase;
import br.com.sicape.api.application.oauth.refresh.RefreshSessionRequest;
import br.com.sicape.api.application.oauth.refresh.RefreshSessionResponse;
import br.com.sicape.api.application.oauth.refresh.RefreshSessionUseCase;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.repository.JudicialDistrictRepository;
import br.com.sicape.api.domain.repository.JudicialProcessRepository;
import br.com.sicape.api.domain.repository.SessionRepository;
import br.com.sicape.api.domain.repository.UserRepository;
import br.com.sicape.api.domain.valueobject.Cpf;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:authentication-integration;MODE=MySQL;NON_KEYWORDS=USER,VALUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=chave-de-teste-com-pelo-menos-32-bytes",
    "jwt.issuer=sicape-api",
    "jwt.access-token-duration=15m"
})
@ActiveProfiles("development")
class AuthenticationIntegrationTest {

    private static final Cpf CPF = Cpf.of("51914372093");
    private static final String PASSWORD = "senha-segura";

    @Autowired
    private CreateSessionUseCase createSessionUseCase;

    @Autowired
    private RefreshSessionUseCase refreshSessionUseCase;

    @Autowired
    private OauthJwtService oauthJwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ConvictedRepository convictedRepository;

    @Autowired
    private JudicialProcessRepository judicialProcessRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JudicialDistrictRepository judicialDistrictRepository;

    @BeforeEach
    void setUp() {
        clearDatabase();

        JudicialDistrict district = new JudicialDistrict();
        district.setName("Comarca de teste");
        judicialDistrictRepository.save(district);

        User user = new User();
        user.setName("Usuário de teste");
        user.setCpf(CPF);
        user.setEmail("teste@sicape.local");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setDistrict(district);
        userRepository.save(user);
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
    void shouldLoginAndRefreshTheCreatedSession() {
        CreateSessionResponse login = createSessionUseCase.execute(
            new CreateSessionRequest(CPF, PASSWORD)
        );

        RefreshSessionResponse refresh = refreshSessionUseCase.execute(
            new RefreshSessionRequest(login.refreshToken())
        );

        assertThat(login.tokenType()).isEqualTo("Bearer");
        assertThat(login.accessToken()).isNotBlank();
        assertThat(login.refreshToken()).isNotBlank();
        assertThat(login.expiresIn()).isEqualTo(900);
        assertThat(sessionRepository.count()).isEqualTo(1);

        assertThat(oauthJwtService.parse(login.accessToken()).getPayload().get("type"))
            .isEqualTo("access");
        assertThat(oauthJwtService.parse(login.accessToken()).getPayload().get("user"))
            .isEqualTo(Map.of(
                "id", userRepository.findByCpf(CPF).orElseThrow().getUuid().toString(),
                "name", "Usuário de teste",
                "cpf", CPF.value()
            ));
        assertThat(oauthJwtService.parse(login.accessToken()).getPayload().get("judicialDistrict"))
            .isEqualTo(Map.of(
                "id", judicialDistrictRepository.findAll().getFirst().getUuid().toString(),
                "name", "Comarca de teste"
            ));
        assertThat(oauthJwtService.parse(login.refreshToken()).getPayload().get("type"))
            .isEqualTo("refresh");
        assertThat(refresh.accessToken()).isNotBlank();
        assertThat(refresh.expiresIn()).isEqualTo(login.expiresIn());
        assertThat(oauthJwtService.parse(refresh.accessToken()).getPayload().get("type"))
            .isEqualTo("access");
        assertThat(oauthJwtService.parse(refresh.accessToken()).getPayload().get("user"))
            .isNotNull();
        assertThat(oauthJwtService.parse(refresh.accessToken()).getPayload().get("judicialDistrict"))
            .isNotNull();
    }

    @Test
    void shouldRejectLoginForInactiveUser() {
        User user = userRepository.findByCpf(CPF).orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> createSessionUseCase.execute(
            new CreateSessionRequest(CPF, PASSWORD)
        )).isInstanceOf(br.com.sicape.api.domain.exception.ForbiddenException.class)
          .hasMessageContaining("Usuário inativo. Procure o administrador.");
    }
}
