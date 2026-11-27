package br.com.sicape.api.application.process;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.oauth.OauthJwtService;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.JudicialProcess;
import br.com.sicape.api.domain.entity.Session;
import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.enums.EmploymentStatus;
import br.com.sicape.api.domain.enums.ProcessStatus;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.repository.JudicialDistrictRepository;
import br.com.sicape.api.domain.repository.JudicialProcessRepository;
import br.com.sicape.api.domain.repository.SessionRepository;
import br.com.sicape.api.domain.repository.UserRepository;
import br.com.sicape.api.domain.valueobject.Address;
import br.com.sicape.api.domain.valueobject.Cpf;

@SpringBootTest(properties = {
    "spring.profiles.active=staging",
    "spring.datasource.url=jdbc:h2:mem:process-tenant-security;MODE=MySQL;NON_KEYWORDS=USER,VALUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=chave-de-teste-com-pelo-menos-32-bytes"
})
@AutoConfigureMockMvc
@Transactional
class ProcessTenantSecurityIntegrationTest {
    private static final String NUMBER = "0001234-56.2026.8.26.0001";
    private static final String SECRET = "chave-de-teste-com-pelo-menos-32-bytes";
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private OauthJwtService jwt;
    @Autowired private JudicialDistrictRepository districts;
    @Autowired private JudicialProcessRepository processes;
    @Autowired private UserRepository users;
    @Autowired private SessionRepository sessions;
    @Autowired private ConvictedRepository convicteds;

    private JudicialDistrict local;
    private JudicialDistrict foreign;
    private User operator;
    private User otherOperator;
    private Session session;
    private String accessToken;

    @BeforeEach
    void setUp() {
        local = district("Comarca local");
        foreign = district("Comarca confidencial");
        operator = users.saveAndFlush(new User("Operador local", Cpf.of("51914372093"),
            "local@test.invalid", "hash", UserRole.OPERATOR, local));
        otherOperator = users.saveAndFlush(new User("Operador externo", Cpf.of("64282587067"),
            "externo@test.invalid", "hash", UserRole.OPERATOR, foreign));
        session = session(operator);
        accessToken = jwt.createAccessToken(session);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "invalid", "expired", "revoked", "inactive", "refresh", "bad-signature"})
    void rejectsUntrustedOrInactiveSessionsWithoutWriting(String kind) throws Exception {
        String token = accessToken;
        switch (kind) {
            case "invalid" -> token = "invalid-token";
            case "expired" -> {
                session.setExpiresAt(Instant.now().minusSeconds(60));
                sessions.saveAndFlush(session);
            }
            case "revoked" -> {
                session.setRevoked(true);
                sessions.saveAndFlush(session);
            }
            case "inactive" -> {
                operator.setActive(false);
                users.saveAndFlush(operator);
            }
            case "refresh" -> token = jwt.createRefreshToken(session);
            case "bad-signature" -> token = signedToken("outra-chave-de-teste-com-mais-de-32-bytes");
            default -> { }
        }
        var request = post("/processes").contentType(MediaType.APPLICATION_JSON)
            .content("{\"number\":\"" + NUMBER + "\"}");
        if (!"missing".equals(kind)) request.header("Authorization", "Bearer " + token);
        mvc.perform(request).andExpect(status().isForbidden());
        assertThat(processes.count()).isZero();
    }

    @Test
    void ignoresForgedDistrictAndMassAssignmentFieldsInBody() throws Exception {
        var response = mvc.perform(post("/processes")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"number":"%s","district_id":"%s","tenant_id":"%s",
                     "district":{"uuid":"%s"},"status":"INACTIVE"}
                    """.formatted(NUMBER, foreign.getUuid(), foreign.getUuid(), foreign.getUuid())))
            .andExpect(status().isCreated()).andReturn().getResponse();
        var id = UUID.fromString(mapper.readTree(response.getContentAsString()).get("id").asText());
        var saved = processes.findByUuid(id).orElseThrow();
        assertThat(saved.getDistrict().getUuid()).isEqualTo(local.getUuid());
        assertThat(saved.getStatus()).isEqualTo(ProcessStatus.ACTIVE);
        mvc.perform(get("/processes").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total_elements").value(1));
        mvc.perform(get("/processes").header("Authorization", "Bearer " + jwt.createAccessToken(session(otherOperator))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total_elements").value(0));
    }

    @Test
    void usesPersistedSessionDistrictInsteadOfTokenDistrictClaims() throws Exception {
        mvc.perform(post("/processes").header("Authorization", "Bearer " + signedToken(SECRET))
                .contentType(MediaType.APPLICATION_JSON).content("{\"number\":\"" + NUMBER + "\"}"))
            .andExpect(status().isCreated());
        assertThat(processes.findAll()).singleElement()
            .satisfies(process -> assertThat(process.getDistrict().getUuid()).isEqualTo(local.getUuid()));
    }

    @Test
    void doesNotReturnForeignProcessOrLinkedNamesForAnySearch() throws Exception {
        processes.saveAndFlush(new JudicialProcess(NUMBER, ProcessStatus.ACTIVE, foreign));
        for (String search : List.of("", NUMBER, "00012345620268260001")) {
            var body = mvc.perform(get("/processes").param("search", search)
                    .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.total_elements").value(0))
                .andReturn().getResponse().getContentAsString();
            assertThat(body).doesNotContain(foreign.getName(), foreign.getUuid().toString());
        }
    }

    @Test
    void rejectsLinkingForeignProcessWhenCreatingOrEditingConvicted() throws Exception {
        var process = processes.saveAndFlush(new JudicialProcess(NUMBER, ProcessStatus.ACTIVE, foreign));
        var existing = convicteds.saveAndFlush(new Convicted("Apenado local", Cpf.of("39053344705"),
            LocalDate.of(1990, 1, 1), null,
            new Address("01001000", "Rua", "10", null, "Centro", "Cidade", "SP"),
            EmploymentStatus.UNEMPLOYED, local));
        String links = "[{\"id\":\"" + process.getUuid() + "\",\"principal\":true}]";
        mvc.perform(post("/convicted").header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Teste","cpf":"93541134780","birth_date":"1990-01-01",
                     "address":{"zip_code":"01001000","street":"Rua","number":"10",
                     "neighborhood":"Centro","city":"Cidade","state":"SP"},"processes":%s}
                    """.formatted(links)))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/convicted/" + existing.getUuid()).header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"processes\":" + links + "}"))
            .andExpect(status().isUnprocessableEntity());
        assertThat(existing.getProcesses()).isEmpty();
        assertThat(convicteds.count()).isEqualTo(1);
    }

    private JudicialDistrict district(String name) {
        var district = new JudicialDistrict();
        district.setName(name);
        return districts.saveAndFlush(district);
    }

    private Session session(User user) {
        var result = new Session();
        result.setUser(user);
        result.setExpiresAt(Instant.now().plusSeconds(3600));
        return sessions.saveAndFlush(result);
    }

    private String signedToken(String key) {
        return Jwts.builder().issuer("sicape-api").id(session.getUuid().toString())
            .subject(otherOperator.getUuid().toString()).claim("type", "access")
            .claim("judicialDistrict", foreign.getUuid().toString())
            .expiration(Date.from(Instant.now().plusSeconds(300)))
            .signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
