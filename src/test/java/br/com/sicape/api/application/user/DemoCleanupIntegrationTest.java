package br.com.sicape.api.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import br.com.sicape.api.domain.entity.*;
import br.com.sicape.api.infrastructure.persistence.seeder.DemoDataCleanup;
import br.com.sicape.api.infrastructure.persistence.util.DevelopmentData;
import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:demo-cleanup;MODE=MySQL;NON_KEYWORDS=USER,VALUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-with-at-least-32-bytes"
})
@ActiveProfiles("development")
class DemoCleanupIntegrationTest {
    @Autowired EntityManager em;

    @Test
    @Transactional
    void removesOnlyPredefinedDemoRecordsAndPreservesAdministratorAndCustomData() {
        var district = new JudicialDistrict();
        district.setName("Custom district");
        em.persist(district);
        em.flush();
        var customUuid = district.getUuid();
        new DemoDataCleanup(em).run();
        em.flush();
        assertThat(em.createQuery("select count(g) from Group g", Long.class).getSingleResult()).isZero();
        assertThat(em.createQuery("select count(c) from Convicted c", Long.class).getSingleResult()).isZero();
        assertThat(em.createQuery("select count(p) from JudicialProcess p", Long.class).getSingleResult()).isZero();
        assertThat(em.createQuery("select u from User u", User.class).getResultList())
            .extracting(User::getUuid).containsExactly(DevelopmentData.mockUuid(100));
        assertThat(em.createQuery("select d.uuid from JudicialDistrict d", java.util.UUID.class).getResultList())
            .containsExactlyInAnyOrder(DevelopmentData.mockUuid(1), customUuid);
    }
}
