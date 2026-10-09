package br.com.sicape.api.application.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import br.com.sicape.api.application.attendance.dto.AttendanceResponse;
import br.com.sicape.api.application.attendance.usecase.GetAttendanceUseCase;
import br.com.sicape.api.application.attendance.usecase.ListAttendanceUseCase;
import br.com.sicape.api.application.attendance.usecase.ListAttendanceYearsUseCase;
import br.com.sicape.api.application.attendance.usecase.ListAttendanceMonthsUseCase;
import br.com.sicape.api.domain.exception.ValidationException;
import java.time.Year;
import java.time.ZoneId;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Attendance;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.JudicialProcess;
import br.com.sicape.api.domain.entity.MediaAsset;
import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.enums.EmploymentStatus;
import br.com.sicape.api.domain.enums.MediaAssetKind;
import br.com.sicape.api.domain.enums.ProcessStatus;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.valueobject.Address;
import br.com.sicape.api.domain.valueobject.Cpf;
import br.com.sicape.api.domain.valueobject.Phone;

@DataJpaTest(showSql = false, properties = {
    "spring.datasource.url=jdbc:h2:mem:attendance-query;MODE=MySQL;NON_KEYWORDS=USER,VALUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ListAttendanceUseCase.class, GetAttendanceUseCase.class, ListAttendanceYearsUseCase.class, ListAttendanceMonthsUseCase.class})
class AttendanceQueryIntegrationTest {
    @Autowired private TestEntityManager entityManager;
    @Autowired private ListAttendanceUseCase listUseCase;
    @Autowired private GetAttendanceUseCase getUseCase;
    @Autowired private ListAttendanceYearsUseCase listYearsUseCase;
    @Autowired private ListAttendanceMonthsUseCase listMonthsUseCase;

    private final Address address = new Address("12345678", "Rua Central", "10", null, "Centro", "Cidade", "SP");
    private final Phone phone = Phone.of("11988881001");
    private AuthContext auth;
    private AuthContext otherAuth;
    private Attendance older;
    private Attendance matching;
    private Attendance latest;
    private Attendance foreign;

    @BeforeEach
    void setUp() {
        auth = createAuth("Comarca Central", "52998224725");
        otherAuth = createAuth("Outra Comarca", "12345678909");
        var convicted = createConvicted("Arthur Morgan", "11144477735", auth);
        var process = entityManager.persistAndFlush(new JudicialProcess(
            "0001234-56.2026.8.26.0001", ProcessStatus.ACTIVE, auth.district()));
        matching = createAttendance(convicted, process, auth, "2026-09-02T12:00:00Z");
        latest = createAttendance(convicted, process, auth, "2026-09-02T12:00:00Z");

        var otherConvicted = createConvicted("Maria Silva", "39053344705", auth);
        var otherProcess = entityManager.persistAndFlush(new JudicialProcess(
            "0009876-54.2026.8.26.0001", ProcessStatus.ACTIVE, auth.district()));
        older = createAttendance(otherConvicted, otherProcess, auth, "2026-09-01T12:00:00Z");

        var foreignConvicted = createConvicted("Arthur Morgan", "16899535009", otherAuth);
        var foreignProcess = entityManager.persistAndFlush(new JudicialProcess(
            "00012345620268260001", ProcessStatus.ACTIVE, otherAuth.district()));
        foreign = createAttendance(foreignConvicted, foreignProcess, otherAuth, "2026-10-01T12:00:00Z");
        entityManager.clear();
    }

    @Test
    void listsOnlyAuthenticatedDistrictWithPaginationAndNewestFirst() {
        var first = listUseCase.execute(null, 0, 2, auth);
        var second = listUseCase.execute(null, 1, 2, auth);

        assertThat(first.content()).extracting(AttendanceResponse::id)
            .containsExactly(latest.getUuid(), matching.getUuid());
        assertThat(first.page()).isZero();
        assertThat(first.size()).isEqualTo(2);
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(second.content()).extracting(AttendanceResponse::id).containsExactly(older.getUuid());
        assertThat(second.page()).isEqualTo(1);
        assertThat(second.totalElements()).isEqualTo(3);
    }

    @Test
    void searchesByNameFormattedCpfAndProcessNumberWithinDistrict() {
        for (String search : new String[]{"  ARTHUR  ", "111.444.777-35", "0001234-56.2026.8.26.0001", "00012345620268260001"}) {
            var result = listUseCase.execute(search, 0, 1, auth);

            assertThat(result.content()).as("Busca por %s", search)
                .extracting(AttendanceResponse::id).containsExactly(latest.getUuid());
            assertThat(result.totalElements()).as("Total da busca por %s", search).isEqualTo(2);
            assertThat(result.totalPages()).isEqualTo(2);
        }
    }

    @Test
    void getsAttendanceDetailsAndRejectsMissingOrOtherDistrictAttendance() {
        var response = getUseCase.execute(matching.getUuid(), auth);

        assertThat(response.id()).isEqualTo(matching.getUuid());
        assertThat(response.convictedId()).isEqualTo(matching.getConvicted().getUuid());
        assertThat(response.processId()).isEqualTo(matching.getProcess().getUuid());
        assertThat(response.userId()).isEqualTo(auth.user().getUuid());
        assertThat(response.userName()).isEqualTo(auth.user().getName());
        assertThat(response.address().street()).isEqualTo("Rua Central");
        assertThat(response.phone()).isEqualTo(phone.value());
        assertThat(response.employmentStatus()).isEqualTo(EmploymentStatus.FORMAL_WORK);
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-09-02T12:00:00Z"));
        assertThatThrownBy(() -> getUseCase.execute(foreign.getUuid(), auth))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> getUseCase.execute(UUID.randomUUID(), auth))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void returnsTheAttendanceAuthorEvenWhenAnotherOperatorReadsIt() {
        var author = entityManager.persistAndFlush(new User("Responsável pelo atendimento", Cpf.of("39053344705"),
            "responsavel@test.local", "hash", UserRole.OPERATOR, auth.district()));
        var record = createAttendance(
            entityManager.find(Convicted.class, matching.getConvicted().getId()),
            entityManager.find(JudicialProcess.class, matching.getProcess().getId()),
            new AuthContext(author, auth.district(), null), "2026-10-02T12:00:00Z");
        entityManager.clear();

        var listed = listUseCase.execute(null, 0, 1, auth).content().getFirst();
        var detail = getUseCase.execute(record.getUuid(), auth);

        assertThat(listed.userId()).isEqualTo(author.getUuid());
        assertThat(listed.userName()).isEqualTo("Responsável pelo atendimento");
        assertThat(detail.userId()).isEqualTo(author.getUuid());
        assertThat(detail.userName()).isEqualTo(listed.userName());
        assertThat(detail.userId()).isNotEqualTo(auth.user().getUuid());
    }

    @Test
    void filtersPeriodBeforePaginationAndCombinesSearchWithinDistrict() {
        var result = listUseCase.execute("Arthur", 2026, 9, 0, 1, auth);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.content()).extracting(AttendanceResponse::id).containsExactly(latest.getUuid());
        assertThat(listUseCase.execute(null, 2026, 10, 0, 20, auth).content()).isEmpty();
        assertThat(listUseCase.execute(null, 2026, null, 0, 20, auth).totalElements()).isEqualTo(3);
        assertThat(listUseCase.execute(null, 2025, null, 0, 20, auth).content()).isEmpty();
    }

    @Test
    void usesSaoPauloMonthAndYearBoundariesWithExclusiveEnd() {
        var convicted = entityManager.find(Convicted.class, matching.getConvicted().getId());
        var process = entityManager.find(JudicialProcess.class, matching.getProcess().getId());
        var before = createAttendance(convicted, process, auth, "2026-01-01T02:59:59Z");
        var start = createAttendance(convicted, process, auth, "2026-01-01T03:00:00Z");
        var end = createAttendance(convicted, process, auth, "2026-02-01T03:00:00Z");
        var nextYear = createAttendance(convicted, process, auth, "2027-01-01T03:00:00Z");
        entityManager.clear();
        assertThat(listUseCase.execute(null, 2025, 12, 0, 20, auth).content())
            .extracting(AttendanceResponse::id).containsExactly(before.getUuid());
        assertThat(listUseCase.execute(null, 2026, 1, 0, 20, auth).content())
            .extracting(AttendanceResponse::id).containsExactly(start.getUuid());
        assertThat(listUseCase.execute(null, 2026, null, 0, 20, auth).content())
            .extracting(AttendanceResponse::id).contains(start.getUuid(), end.getUuid())
            .doesNotContain(before.getUuid(), nextYear.getUuid());
    }

    @Test
    void rejectsInvalidPeriods() {
        for (Integer month : new Integer[]{0, 13}) {
            assertThatThrownBy(() -> listUseCase.execute(null, 2026, month, 0, 20, auth))
                .isInstanceOf(ValidationException.class);
        }
        assertThatThrownBy(() -> listUseCase.execute(null, null, 1, 0, 20, auth))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> listUseCase.execute(null, 0, null, 0, 20, auth))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> listUseCase.execute(null, 9999, null, 0, 20, auth))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void countsAllDocumentsPerLocalMonthWithoutOtherDistricts() {
        assertThat(listMonthsUseCase.execute(2026, auth).counts())
            .containsExactly(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 3L, 0L, 0L, 0L);
        var convicted = entityManager.find(Convicted.class, matching.getConvicted().getId());
        var process = entityManager.find(JudicialProcess.class, matching.getProcess().getId());
        createAttendance(convicted, process, auth, "2026-02-01T02:59:59Z");
        createAttendance(convicted, process, auth, "2026-02-01T03:00:00Z");
        entityManager.clear();
        var counts = listMonthsUseCase.execute(2026, auth).counts();
        assertThat(counts.get(0)).isEqualTo(1L);
        assertThat(counts.get(1)).isEqualTo(1L);
        assertThat(counts.stream().mapToLong(Long::longValue).sum())
            .isEqualTo(listUseCase.execute(null, 2026, null, 0, 1, auth).totalElements());
        assertThat(listMonthsUseCase.execute(2025, auth).counts()).containsOnly(0L);
        assertThatThrownBy(() -> listMonthsUseCase.execute(0, auth)).isInstanceOf(ValidationException.class);
    }

    @Test
    void yearsIncludeCurrentAndGapsButExcludeOtherDistricts() {
        var convicted = entityManager.find(Convicted.class, matching.getConvicted().getId());
        var process = entityManager.find(JudicialProcess.class, matching.getProcess().getId());
        createAttendance(convicted, process, auth, "2024-01-01T02:59:59Z");
        createAttendance(entityManager.find(Convicted.class, foreign.getConvicted().getId()),
            entityManager.find(JudicialProcess.class, foreign.getProcess().getId()), otherAuth, "2020-01-01T12:00:00Z");
        entityManager.clear();
        int current = Year.now(ZoneId.of("America/Sao_Paulo")).getValue();
        var years = listYearsUseCase.execute(auth).years();
        assertThat(years).contains(2023, 2024, 2025, 2026, current).doesNotContain(2020);
        assertThat(years).isSortedAccordingTo(java.util.Comparator.reverseOrder());
        var emptyAuth = createAuth("Comarca vazia", "39053344705");
        assertThat(listYearsUseCase.execute(emptyAuth).years()).containsExactly(current);
    }

    private AuthContext createAuth(String name, String cpf) {
        var district = new JudicialDistrict();
        district.setName(name);
        entityManager.persistAndFlush(district);
        var user = entityManager.persistAndFlush(new User("Operador", Cpf.of(cpf),
            cpf + "@test.local", "hash", UserRole.OPERATOR, district));
        return new AuthContext(user, district, null);
    }

    private Convicted createConvicted(String name, String cpf, AuthContext context) {
        return entityManager.persistAndFlush(new Convicted(name, Cpf.of(cpf), LocalDate.of(1990, 1, 1),
            phone, address, EmploymentStatus.FORMAL_WORK, context.district()));
    }

    private Attendance createAttendance(Convicted convicted, JudicialProcess process, AuthContext context, String createdAt) {
        var photo = entityManager.persistAndFlush(new MediaAsset(UUID.randomUUID(), UUID.randomUUID().toString(),
            "image/png", 3, MediaAssetKind.PHOTO));
        var attendance = entityManager.persistAndFlush(new Attendance(convicted, process, context.district(),
            context.user(), address, phone, EmploymentStatus.FORMAL_WORK, photo));
        entityManager.getEntityManager().createNativeQuery("update attendance set created_at = :createdAt where id = :id")
            .setParameter("createdAt", Timestamp.from(Instant.parse(createdAt)))
            .setParameter("id", attendance.getId())
            .executeUpdate();
        return attendance;
    }
}
