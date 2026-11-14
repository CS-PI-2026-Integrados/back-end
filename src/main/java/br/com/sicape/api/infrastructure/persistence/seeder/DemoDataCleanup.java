package br.com.sicape.api.infrastructure.persistence.seeder;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import br.com.sicape.api.domain.entity.*;
import br.com.sicape.api.infrastructure.persistence.util.DevelopmentData;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Explicit, idempotent cleanup of the exact sample UUIDs, never customer records. */
@Component
@Order(0)
@ConditionalOnProperty(name = "DEMO_CLEANUP_ENABLED", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataCleanup implements CommandLineRunner {
    private final EntityManager em;

    @Override
    @Transactional
    public void run(String... args) {
        // FK constraints intentionally abort the transaction if non-demo data depends on a sample.
        for (int id : List.of(400, 401, 402)) remove(Group.class, id);
        em.flush();
        for (int id : List.of(300, 301, 302)) remove(Convicted.class, id);
        em.flush();
        for (int id : List.of(200, 201, 202)) remove(JudicialProcess.class, id);
        em.flush();
        for (int id : List.of(101, 102)) {
            em.createQuery("delete from Session s where s.user.uuid = :uuid")
                .setParameter("uuid", DevelopmentData.mockUuid(id)).executeUpdate();
            remove(User.class, id);
        }
        em.flush();
        for (int id : List.of(2, 3)) remove(JudicialDistrict.class, id);
        log.info("Cleanup of predefined demo records completed; administrator and its district preserved.");
    }

    private <T> void remove(Class<T> type, int id) {
        UUID uuid = DevelopmentData.mockUuid(id);
        em.createQuery("select e from " + type.getSimpleName() + " e where e.uuid = :uuid", type)
            .setParameter("uuid", uuid).getResultStream().forEach(em::remove);
    }
}
