package br.com.sicape.api.infrastructure.persistence.seeder;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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
    private final PlatformTransactionManager transactionManager;

    @Override
    public void run(String... args) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> cleanup());
        } catch (RuntimeException exception) {
            // Preserve availability and data: any unexpected dependency rolls back all changes.
            log.warn("Demo cleanup rolled back due to an existing dependency; no records removed.");
        }
    }

    private void cleanup() {
        for (int id : List.of(400, 401, 402)) remove(Group.class, id);
        em.flush();
        for (int id : List.of(300, 301, 302)) {
            if (count("select count(a) from Attendance a where a.convicted.uuid = :uuid", id) == 0 &&
                count("select count(g) from Group g join g.convicteds c where c.uuid = :uuid", id) == 0) {
                remove(Convicted.class, id);
            }
        }
        em.flush();
        for (int id : List.of(200, 201, 202)) {
            if (count("select count(c) from ConvictedProcess c where c.process.uuid = :uuid", id) == 0 &&
                count("select count(a) from Attendance a where a.process.uuid = :uuid", id) == 0) {
                remove(JudicialProcess.class, id);
            }
        }
        em.flush();
        for (int id : List.of(101, 102)) {
            if (count("select count(a) from Attendance a where a.user.uuid = :uuid", id) > 0 ||
                count("select count(c) from Convicted c where c.deactivatedBy.uuid = :uuid", id) > 0) continue;
            em.createQuery("delete from Session s where s.user.uuid = :uuid")
                .setParameter("uuid", DevelopmentData.mockUuid(id)).executeUpdate();
            remove(User.class, id);
        }
        em.flush();
        for (int id : List.of(2, 3)) {
            boolean referenced = false;
            for (String entity : List.of("User", "Convicted", "JudicialProcess", "Group", "Attendance")) {
                referenced |= count("select count(e) from " + entity + " e where e.district.uuid = :uuid", id) > 0;
            }
            if (!referenced) remove(JudicialDistrict.class, id);
        }
        em.flush();
        log.info("Cleanup of unreferenced demo records completed; administrator and referenced records preserved.");
    }

    private long count(String jpql, int id) {
        return em.createQuery(jpql, Long.class).setParameter("uuid", DevelopmentData.mockUuid(id)).getSingleResult();
    }

    private <T> void remove(Class<T> type, int id) {
        UUID uuid = DevelopmentData.mockUuid(id);
        em.createQuery("select e from " + type.getSimpleName() + " e where e.uuid = :uuid", type)
            .setParameter("uuid", uuid).getResultStream().forEach(em::remove);
    }
}
