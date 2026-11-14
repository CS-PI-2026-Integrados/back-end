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
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Test
    @Transactional
    void removesOnlyPredefinedDemoRecordsAndPreservesAdministratorAndCustomData() {
        var district = new JudicialDistrict();
        district.setName("Custom district");
        em.persist(district);
        em.flush();
        var customUuid = district.getUuid();
        new DemoDataCleanup(em, transactionManager).run();
        em.flush();
        assertThat(em.createQuery("select count(g) from Group g", Long.class).getSingleResult()).isZero();
        assertThat(em.createQuery("select count(c) from Convicted c", Long.class).getSingleResult()).isZero();
        assertThat(em.createQuery("select count(p) from JudicialProcess p", Long.class).getSingleResult()).isZero();
        assertThat(em.createQuery("select u from User u", User.class).getResultList())
            .extracting(User::getUuid).containsExactly(DevelopmentData.mockUuid(100));
        assertThat(em.createQuery("select d.uuid from JudicialDistrict d", java.util.UUID.class).getResultList())
            .containsExactlyInAnyOrder(DevelopmentData.mockUuid(1), customUuid);
    }

    @Test
    @Transactional
    void preservesDemoProcessReferencedByCustomConvicted() {
        var process = em.createQuery("select p from JudicialProcess p where p.uuid = :uuid", JudicialProcess.class)
            .setParameter("uuid", DevelopmentData.mockUuid(200)).getSingleResult();
        var custom = new Convicted("Custom record", br.com.sicape.api.domain.valueobject.Cpf.of("11144477735"),
            null, null, new br.com.sicape.api.domain.valueobject.Address("01001000", "Test street", "1", null,
                "Test neighborhood", "Test city", "SP"),
            br.com.sicape.api.domain.enums.EmploymentStatus.UNEMPLOYED, process.getDistrict());
        em.persist(custom);
        em.persist(new ConvictedProcess(custom, process, true));
        em.flush();
        var customUuid = custom.getUuid();
        new DemoDataCleanup(em, transactionManager).run();
        em.flush();
        assertThat(em.createQuery("select count(c) from Convicted c where c.uuid = :uuid", Long.class)
            .setParameter("uuid", customUuid).getSingleResult()).isEqualTo(1);
        assertThat(em.createQuery("select count(p) from JudicialProcess p where p.uuid = :uuid", Long.class)
            .setParameter("uuid", process.getUuid()).getSingleResult()).isEqualTo(1);
    }
}
