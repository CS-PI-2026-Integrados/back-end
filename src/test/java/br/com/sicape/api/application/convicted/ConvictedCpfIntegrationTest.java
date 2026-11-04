package br.com.sicape.api.application.convicted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import br.com.sicape.api.application.oauth.OauthJwtService;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.Session;
import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.enums.ConvictedStatus;
import br.com.sicape.api.domain.enums.EmploymentStatus;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.repository.GroupRepository;
import br.com.sicape.api.domain.repository.JudicialDistrictRepository;
import br.com.sicape.api.domain.repository.SessionRepository;
import br.com.sicape.api.domain.repository.UserRepository;
import br.com.sicape.api.domain.valueobject.Address;
import br.com.sicape.api.domain.valueobject.Cpf;
import br.com.sicape.api.domain.valueobject.Phone;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:convicted-cpf-integration;MODE=MySQL;NON_KEYWORDS=USER,VALUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=chave-de-teste-com-pelo-menos-32-bytes",
    "jwt.issuer=sicape-api",
    "jwt.access-token-duration=15m"
})
@ActiveProfiles("staging")
@AutoConfigureMockMvc
class ConvictedCpfIntegrationTest {
    private static final String CPF = "52998224725";
    private static final String MASKED_CPF = "***.982.247-**";
    private static final String PHONE = "11988881001";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OauthJwtService jwtService;
    @Autowired private ConvictedRepository convicteds;
    @Autowired private GroupRepository groups;
    @Autowired private SessionRepository sessions;
    @Autowired private UserRepository users;
    @Autowired private JudicialDistrictRepository districts;

    private User admin;
    private User operator;
    private Convicted active;
    private Convicted inactive;
    private Convicted foreign;

    @BeforeEach
    void setUp() {
        groups.deleteAll();
        convicteds.deleteAll();
        sessions.deleteAll();
        users.deleteAll();
        districts.deleteAll();

        var district = district("Comarca de atendimento");
        var otherDistrict = district("Outra comarca");
        admin = user("51914372093", UserRole.ADMIN, district);
        operator = user("64282587067", UserRole.OPERATOR, district);
        active = convicted("Apenado ativo", CPF, district);
        inactive = convicted("Apenado inativo", "39053344705", district);
        inactive.updateStatus(ConvictedStatus.INACTIVE, operator);
        convicteds.saveAndFlush(inactive);
        foreign = convicted("Apenado de outra comarca", "11144477735", otherDistrict);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ADMIN", "OPERATOR"})
    void revealsOnlyCpfForActiveAndInactiveConvictedsInOwnDistrict(UserRole role) throws Exception {
        var token = credentials(role == UserRole.ADMIN ? admin : operator).accessToken();

        for (var convicted : List.of(active, inactive)) {
            reveal(convicted.getUuid(), token)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$", aMapWithSize(1)))
                .andExpect(jsonPath("$.cpf").value(convicted.getCpf().value()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "malformed", "expired-access", "refresh", "missing-session",
        "revoked-session", "expired-session", "inactive-user"})
    void rejectsInvalidAuthenticationWithoutReturningCpf(String scenario) throws Exception {
        var credentials = credentials(operator);
        String token = credentials.accessToken();
        switch (scenario) {
            case "missing" -> token = null;
            case "malformed" -> token = "invalid-token";
            case "expired-access" -> token = expiredToken(credentials.session());
            case "refresh" -> token = credentials.refreshToken();
            case "missing-session" -> sessions.delete(credentials.session());
            case "revoked-session" -> revoke(credentials.session());
            case "expired-session" -> {
                credentials.session().setExpiresAt(Instant.now().minusSeconds(60));
                sessions.saveAndFlush(credentials.session());
            }
            case "inactive-user" -> {
                operator.setActive(false);
                users.saveAndFlush(operator);
            }
            default -> throw new IllegalArgumentException(scenario);
        }

        reveal(active.getUuid(), token)
            .andExpect(status().isForbidden())
            .andExpect(content().string(not(containsString(CPF))));
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ADMIN", "OPERATOR"})
    void treatsForeignAndMissingConvictedsAsNotFound(UserRole role) throws Exception {
        var token = credentials(role == UserRole.ADMIN ? admin : operator).accessToken();
        for (var id : List.of(foreign.getUuid(), UUID.randomUUID())) {
            reveal(id, token)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Condenado não encontrado."))
                .andExpect(content().string(not(containsString(foreign.getCpf().value()))));
        }
    }

    @Test
    void rejectsMalformedUuid() throws Exception {
        mvc.perform(get("/convicted/not-a-uuid/cpf").header("Authorization", bearer()))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void revalidatesSessionAfterAReveal() throws Exception {
        var credentials = credentials(operator);
        reveal(active.getUuid(), credentials.accessToken()).andExpect(status().isOk());

        revoke(credentials.session());

        reveal(active.getUuid(), credentials.accessToken())
            .andExpect(status().isForbidden())
            .andExpect(content().string(not(containsString(CPF))));
    }

    @Test
    void usesCurrentAuthenticatedDistrictInsteadOfTokenClaims() throws Exception {
        var token = credentials(operator).accessToken();
        operator.setDistrict(foreign.getDistrict());
        users.saveAndFlush(operator);

        reveal(active.getUuid(), token).andExpect(status().isNotFound());
        reveal(foreign.getUuid(), token)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cpf").value(foreign.getCpf().value()));
    }

    @Test
    void standardReadsStayMaskedBeforeAndAfterAReveal() throws Exception {
        var token = credentials(operator).accessToken();
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(get("/convicted").param("search", CPF)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].cpf").value(MASKED_CPF))
                .andExpect(jsonPath("$.content[0].phone").value(PHONE))
                .andExpect(content().string(not(containsString(CPF))));
            expectMasked(mvc.perform(get("/convicted/{id}", active.getUuid())
                .header("Authorization", "Bearer " + token)), MASKED_CPF, CPF);
            reveal(active.getUuid(), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(CPF));
        }
    }

    @Test
    void createsWithCompleteCpfButReturnsOnlyMaskAndCompletePhone() throws Exception {
        var response = mvc.perform(post("/convicted").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "Novo apenado", "cpf", "123.456.789-09", "birth_date", "1990-01-01",
                    "phone", PHONE, "address", Map.of("zip_code", "01001000", "street", "Rua Central",
                        "number", "10", "neighborhood", "Centro", "city", "Cidade", "state", "SP")))))
            .andExpect(status().isCreated());
        expectMasked(response, "***.456.789-**", "12345678909");

        var id = UUID.fromString(objectMapper.readTree(response.andReturn().getResponse()
            .getContentAsString()).get("id").asText());
        assertThat(convicteds.findByUuid(id).orElseThrow().getCpf().value()).isEqualTo("12345678909");
    }

    @Test
    void updatingOtherFieldsPreservesStoredCpfAndReturnsMask() throws Exception {
        expectMasked(mvc.perform(put("/convicted/{id}", active.getUuid())
            .header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Nome atualizado\"}")), MASKED_CPF, CPF);

        var saved = convicteds.findByUuid(active.getUuid()).orElseThrow();
        assertThat(saved.getName()).isEqualTo("Nome atualizado");
        assertThat(saved.getCpf().value()).isEqualTo(CPF);
    }

    @Test
    void acceptsExplicitCompleteCpfUpdateButOnlyReturnsMask() throws Exception {
        var token = credentials(operator).accessToken();
        expectMasked(mvc.perform(put("/convicted/{id}", active.getUuid())
            .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
            .content("{\"cpf\":\"168.995.350-09\"}")), "***.995.350-**", "16899535009");

        assertThat(convicteds.findByUuid(active.getUuid()).orElseThrow().getCpf().value())
            .isEqualTo("16899535009");
        reveal(active.getUuid(), token).andExpect(status().isOk())
            .andExpect(jsonPath("$.cpf").value("16899535009"));
    }

    @Test
    void rejectsMaskedCpfUpdateWithoutChangingStoredValue() throws Exception {
        mvc.perform(put("/convicted/{id}", active.getUuid())
                .header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("cpf", MASKED_CPF))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.fields[0].field").value("cpf"));
        assertThat(convicteds.findByUuid(active.getUuid()).orElseThrow().getCpf().value()).isEqualTo(CPF);
    }

    @Test
    void statusChangesReturnMaskAndCompletePhone() throws Exception {
        var bearer = bearer();
        for (var status : List.of(ConvictedStatus.INACTIVE, ConvictedStatus.ACTIVE)) {
            expectMasked(mvc.perform(put("/convicted/{id}/status/{status}", active.getUuid(), status)
                .header("Authorization", bearer)), MASKED_CPF, CPF)
                .andExpect(jsonPath("$.status").value(status.name()));
        }
    }

    @Test
    void groupCreationDetailAndUpdateOnlyReturnMaskedParticipantCpf() throws Exception {
        var bearer = bearer();
        var created = mvc.perform(post("/group").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "Grupo de teste", "description", "Atendimento", "subject", "Reflexão",
                    "presenters", List.of("Ministrante"), "total_meetings_count", 8,
                    "minimum_meetings_count", 4, "meeting_base_time", "14:00", "frequency", "WEEKLY",
                    "start_date", "2026-01-01", "convicted_uuids", List.of(active.getUuid())))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.convicteds[0].document").value(MASKED_CPF))
            .andExpect(content().string(not(containsString(CPF))));
        var id = objectMapper.readTree(created.andReturn().getResponse().getContentAsString())
            .get("uuid").asText();

        for (var request : List.of(get("/group/{id}", id),
            put("/group/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Grupo atualizado\"}"))) {
            mvc.perform(request.header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convicteds[0].document").value(MASKED_CPF))
                .andExpect(content().string(not(containsString(CPF))));
        }
    }

    private ResultActions expectMasked(ResultActions response, String mask, String fullCpf) throws Exception {
        return response.andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.cpf").value(mask))
            .andExpect(jsonPath("$.phone").value(PHONE))
            .andExpect(content().string(not(containsString(fullCpf))));
    }

    private ResultActions reveal(UUID id, String token) throws Exception {
        MockHttpServletRequestBuilder request = get("/convicted/{id}/cpf", id);
        if (token != null) request.header("Authorization", "Bearer " + token);
        return mvc.perform(request);
    }

    private String bearer() {
        return "Bearer " + credentials(operator).accessToken();
    }

    private Credentials credentials(User user) {
        var session = new Session();
        session.setUser(user);
        session.setExpiresAt(Instant.now().plusSeconds(3600));
        sessions.saveAndFlush(session);
        return new Credentials(session, jwtService.createAccessToken(session), jwtService.createRefreshToken(session));
    }

    private void revoke(Session session) {
        session.setRevoked(true);
        sessions.saveAndFlush(session);
    }

    private String expiredToken(Session session) {
        return Jwts.builder().issuer("sicape-api").id(session.getUuid().toString()).claim("type", "access")
            .expiration(Date.from(Instant.now().minusSeconds(60)))
            .signWith(Keys.hmacShaKeyFor("chave-de-teste-com-pelo-menos-32-bytes".getBytes(StandardCharsets.UTF_8)))
            .compact();
    }

    private JudicialDistrict district(String name) {
        var district = new JudicialDistrict();
        district.setName(name);
        return districts.saveAndFlush(district);
    }

    private User user(String cpf, UserRole role, JudicialDistrict district) {
        return users.saveAndFlush(new User(role.name(), Cpf.of(cpf), cpf + "@test.local", "hash", role, district));
    }

    private Convicted convicted(String name, String cpf, JudicialDistrict district) {
        return convicteds.saveAndFlush(new Convicted(name, Cpf.of(cpf), LocalDate.of(1990, 1, 1),
            Phone.of(PHONE), new Address("01001000", "Rua Central", "10", null, "Centro", "Cidade", "SP"),
            EmploymentStatus.FORMAL_WORK, district));
    }

    private record Credentials(Session session, String accessToken, String refreshToken) {}
}
