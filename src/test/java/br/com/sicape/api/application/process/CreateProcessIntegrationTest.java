package br.com.sicape.api.application.process;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.application.process.dto.request.CreateProcessRequest;
import br.com.sicape.api.application.process.usecase.CreateProcessUseCase;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.JudicialProcess;
import br.com.sicape.api.domain.enums.ProcessStatus;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.repository.JudicialProcessRepository;

@DataJpaTest(showSql = false, properties = {
    "spring.datasource.url=jdbc:h2:mem:process-create;MODE=MySQL;NON_KEYWORDS=USER,VALUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CreateProcessUseCase.class)
class CreateProcessIntegrationTest {
    @Autowired private CreateProcessUseCase useCase;
    @Autowired private JudicialProcessRepository repository;
    @Autowired private TestEntityManager entityManager;

    @Test
    void persistsCanonicalNumberAndRejectsFormattedOrUnformattedDuplicates() {
        var district = district("Comarca local");
        var auth = new AuthContext(null, district, null);
        var created = useCase.execute(new CreateProcessRequest("00012345620268260001"), auth);
        entityManager.clear();
        var saved = repository.findByUuid(created.id()).orElseThrow();
        assertThat(saved.getNumber()).isEqualTo("0001234-56.2026.8.26.0001");
        assertThat(saved.getDistrict().getUuid()).isEqualTo(district.getUuid());
        assertThatThrownBy(() -> useCase.execute(new CreateProcessRequest(saved.getNumber()), auth))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsLegacyUnformattedInactiveNumberFromAnotherDistrict() {
        var district = district("Outra comarca");
        repository.saveAndFlush(new JudicialProcess("00012345620268260001", ProcessStatus.INACTIVE, district));
        var auth = new AuthContext(null, district("Comarca local"), null);
        assertThatThrownBy(() -> useCase.execute(new CreateProcessRequest("0001234-56.2026.8.26.0001"), auth))
            .isInstanceOf(ConflictException.class);
    }

    private JudicialDistrict district(String name) {
        var district = new JudicialDistrict();
        district.setName(name);
        return entityManager.persistAndFlush(district);
    }
}
