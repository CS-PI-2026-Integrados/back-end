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
@Import({ListAttendanceUseCase.class, GetAttendanceUseCase.class})
class AttendanceQueryIntegrationTest {
    @Autowired private TestEntityManager entityManager;
    @Autowired private ListAttendanceUseCase listUseCase;
    @Autowired private GetAttendanceUseCase getUseCase;

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
        assertThat(response.address().street()).isEqualTo("Rua Central");
        assertThat(response.phone()).isEqualTo(phone.value());
        assertThat(response.employmentStatus()).isEqualTo(EmploymentStatus.FORMAL_WORK);
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-09-02T12:00:00Z"));
        assertThatThrownBy(() -> getUseCase.execute(foreign.getUuid(), auth))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> getUseCase.execute(UUID.randomUUID(), auth))
            .isInstanceOf(ResourceNotFoundException.class);
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
