package br.com.sicape.api.application.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import br.com.sicape.api.application.attendance.dto.AttendanceMetricsResponse;
import br.com.sicape.api.application.attendance.usecase.GetAttendanceMetricsUseCase;
import br.com.sicape.api.application.convicted.usecase.GetConvictedMetricsUseCase;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.*;
import br.com.sicape.api.domain.enums.*;
import br.com.sicape.api.domain.valueobject.*;

@DataJpaTest(showSql = false, properties = {
    "spring.datasource.url=jdbc:h2:mem:metrics;MODE=MySQL;NON_KEYWORDS=USER,VALUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GetConvictedMetricsUseCase.class, GetAttendanceMetricsUseCase.class})
class MetricsIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");
    private final Address address = new Address("12345678", "Rua Central", "10", null, "Centro", "Cidade", "SP");
    private final Phone phone = Phone.of("11988881001");
    @Autowired private TestEntityManager em;
    @Autowired private GetConvictedMetricsUseCase convictedMetrics;
    @Autowired private GetAttendanceMetricsUseCase attendanceMetrics;
    @MockitoBean private Clock clock;
    private AuthContext auth;
    private JudicialProcess process;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);
        auth = createAuth("Central", "52998224725");
        // Process status does not change attendance-based classification.
        process = em.persistAndFlush(new JudicialProcess("0001234-56.2026.8.26.0001", ProcessStatus.INACTIVE, auth.district()));
    }

    @Test
    void classifiesOnlyActivePeopleOnceAndCountsAttendancesWithoutPdfsWithinDistrict() {
        var recent = person("Recente", "11144477735", auth);
        var stale = person("Antigo", "39053344705", auth);
        person("Sem atendimento", "16899535009", auth);
        var future = person("Futuro", "12345678909", auth);
        var inactive = person("Inativo", "51914372093", auth);
        inactive.updateStatus(ConvictedStatus.INACTIVE, auth.user());
        attendance(recent, process, auth, NOW.minus(30, ChronoUnit.DAYS));
        attendance(recent, process, auth, NOW.minus(7, ChronoUnit.DAYS));
        attendance(stale, process, auth, NOW.minus(30, ChronoUnit.DAYS).minusMillis(1));
        attendance(future, process, auth, NOW);
        attendance(future, process, auth, NOW.plusSeconds(1));
        attendance(inactive, process, auth, NOW.minus(1, ChronoUnit.DAYS));
        var foreignAuth = createAuth("Outra", "12345678909");
        var foreign = person("Outra comarca", "52998224725", foreignAuth);
        var foreignProcess = em.persistAndFlush(new JudicialProcess("0009876-54.2026.8.26.0001", ProcessStatus.ACTIVE, foreignAuth.district()));
        attendance(foreign, foreignProcess, foreignAuth, NOW.minusSeconds(1));
        // Even an inconsistent historical district must not affect the person's classification.
        attendance(future, foreignProcess, foreignAuth, NOW.minusSeconds(1));
        em.flush();
        em.clear();

        var people = convictedMetrics.execute(auth);
        assertThat(people.total()).isEqualTo(5);
        assertThat(people.active()).isEqualTo(4);
        assertThat(people.inactive()).isEqualTo(1);
        assertThat(people.withRecentAttendance()).isEqualTo(1);
        assertThat(people.withoutRecentAttendance()).isEqualTo(3);
        assertThat(people.calculatedAt()).isEqualTo(NOW);

        var records = attendanceMetrics.execute(auth);
        assertThat(records.last7Days()).isEqualTo(2);
        assertThat(records.recentActivities()).extracting(AttendanceMetricsResponse.RecentActivity::convictedName)
            .containsExactly("Inativo", "Recente", "Recente", "Antigo");
        assertThat(records.calculatedAt()).isEqualTo(NOW);
    }

    @Test
    void excludesJustBeforeSevenDayBoundaryAndIncludesBoundaryRegardlessOfRepeatedPerson() {
        var person = person("Pessoa", "11144477735", auth);
        attendance(person, process, auth, NOW.minus(7, ChronoUnit.DAYS).minusMillis(1));
        attendance(person, process, auth, NOW.minus(7, ChronoUnit.DAYS));
        attendance(person, process, auth, NOW.minusSeconds(1));
        attendance(person, process, auth, NOW);
        em.clear();
        assertThat(attendanceMetrics.execute(auth).last7Days()).isEqualTo(2);
        assertThat(convictedMetrics.execute(auth).withRecentAttendance()).isEqualTo(1);
    }

    @Test
    void countsSixLocalMonthsIncludingZerosAtUtcYearBoundaryAndExcludesFutureRecords() {
        var now = Instant.parse("2027-01-01T02:30:00Z"); // Still December in São Paulo.
        when(clock.instant()).thenReturn(now);
        var person = person("Pessoa", "11144477735", auth);
        for (String date : List.of("2026-07-01T02:59:59Z", "2026-07-01T03:00:00Z",
                "2026-08-01T02:59:59Z", "2026-08-01T03:00:00Z",
                "2026-12-01T02:59:59Z", "2026-12-01T03:00:00Z",
                "2027-01-01T02:29:59Z", "2027-01-01T02:30:00Z", "2027-01-01T03:00:00Z")) {
            attendance(person, process, auth, Instant.parse(date));
        }
        em.clear();
        var result = attendanceMetrics.execute(auth);
        assertThat(result.monthlyCounts()).extracting(AttendanceMetricsResponse.MonthlyCount::month)
            .containsExactly("2026-07", "2026-08", "2026-09", "2026-10", "2026-11", "2026-12");
        assertThat(result.monthlyCounts()).extracting(AttendanceMetricsResponse.MonthlyCount::count)
            .containsExactly(2L, 1L, 0L, 0L, 1L, 2L);
    }

    @Test
    void returnsFourNewestActivitiesWithStableTieBreakWithoutLoadingEntities() {
        var person = person("Nome atual", "11144477735", auth);
        var expected = new ArrayList<UUID>();
        for (int i = 0; i < 6; i++) {
            expected.add(attendance(person, process, auth, NOW.minusSeconds(1)).getUuid());
        }
        em.clear();
        var stats = em.getEntityManager().getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        var records = attendanceMetrics.execute(auth);
        assertThat(records.recentActivities()).extracting(AttendanceMetricsResponse.RecentActivity::id)
            .containsExactly(expected.get(5), expected.get(4), expected.get(3), expected.get(2));
        assertThat(records.recentActivities()).allSatisfy(item -> {
            assertThat(item.convictedId()).isEqualTo(person.getUuid());
            assertThat(item.convictedName()).isEqualTo("Nome atual");
        });
        assertThat(stats.getEntityLoadCount()).isZero();
        assertThat(stats.getPrepareStatementCount()).isEqualTo(8);
    }

    @Test
    void returnsEmptyMetricsAndSixMonthsAcrossYearBoundary() {
        when(clock.instant()).thenReturn(Instant.parse("2027-03-15T12:00:00Z"));
        var people = convictedMetrics.execute(auth);
        assertThat(List.of(people.total(), people.active(), people.inactive(),
            people.withRecentAttendance(), people.withoutRecentAttendance())).containsOnly(0L);
        var records = attendanceMetrics.execute(auth);
        assertThat(records.last7Days()).isZero();
        assertThat(records.recentActivities()).isEmpty();
        assertThat(records.monthlyCounts()).extracting(AttendanceMetricsResponse.MonthlyCount::month)
            .containsExactly("2026-10", "2026-11", "2026-12", "2027-01", "2027-02", "2027-03");
        assertThat(records.monthlyCounts()).extracting(AttendanceMetricsResponse.MonthlyCount::count).containsOnly(0L);
    }

    private AuthContext createAuth(String name, String cpf) {
        var district = new JudicialDistrict();
        district.setName(name);
        em.persistAndFlush(district);
        var user = em.persistAndFlush(new User("Operador", Cpf.of(cpf), cpf + "@test.local", "hash", UserRole.OPERATOR, district));
        return new AuthContext(user, district, null);
    }

    private Convicted person(String name, String cpf, AuthContext context) {
        return em.persistAndFlush(new Convicted(name, Cpf.of(cpf), LocalDate.of(1990, 1, 1),
            phone, address, EmploymentStatus.FORMAL_WORK, context.district()));
    }

    private Attendance attendance(Convicted convicted, JudicialProcess linkedProcess, AuthContext context, Instant createdAt) {
        var photo = em.persistAndFlush(new MediaAsset(UUID.randomUUID(), UUID.randomUUID().toString(), "image/png", 3, MediaAssetKind.PHOTO));
        var attendance = em.persistAndFlush(new Attendance(convicted, linkedProcess, context.district(), context.user(),
            address, phone, EmploymentStatus.FORMAL_WORK, photo));
        em.getEntityManager().createNativeQuery("update attendance set created_at = :createdAt where id = :id")
            .setParameter("createdAt", Timestamp.from(createdAt)).setParameter("id", attendance.getId()).executeUpdate();
        return attendance;
    }
}
