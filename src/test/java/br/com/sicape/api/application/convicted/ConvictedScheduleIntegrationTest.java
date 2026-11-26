package br.com.sicape.api.application.convicted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import br.com.sicape.api.application.convicted.schedule.*;
import br.com.sicape.api.application.convicted.schedule.ConvictedScheduleEvent.Origin;
import br.com.sicape.api.application.convicted.schedule.ConvictedScheduleEvent.Status;
import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.convicted.usecase.GetConvictedScheduleUseCase;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.*;
import br.com.sicape.api.domain.enums.*;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.valueobject.*;

@DataJpaTest(showSql = false, properties = {
    "spring.datasource.url=jdbc:h2:mem:convicted-schedule;MODE=MySQL;NON_KEYWORDS=USER,VALUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GetConvictedScheduleUseCase.class, ConvictedFinder.class,
    GroupConvictedScheduleSource.class, AttendanceConvictedScheduleSource.class})
class ConvictedScheduleIntegrationTest {
    private static final LocalDate FROM = LocalDate.of(2026, 10, 1);
    private static final LocalDate TO = FROM.plusMonths(1);
    private final Address address = new Address("12345678", "Rua Central", "10", null, "Centro", "Cidade", "SP");
    @Autowired private TestEntityManager em;
    @Autowired private GetConvictedScheduleUseCase useCase;
    private AuthContext auth;
    private Convicted person;
    private Convicted homonym;

    @BeforeEach
    void setUp() {
        auth = auth("Comarca Central", "52998224725");
        person = person(auth, "11144477735");
        homonym = person(auth, "39053344705");
    }

    @Test
    void combinesTwoGroupsAndAppearancesOnSameDayWithoutCurrentProcessesOrOtherParticipants() {
        var day = FROM.plusDays(5);
        var first = meeting(group(auth, "Grupo A", person, FROM, null), day);
        var second = meeting(group(auth, "Grupo B", person, FROM, null), day);
        meeting(group(auth, "Grupo do homônimo", homonym, FROM, null), day);
        var process = em.persistAndFlush(new JudicialProcess("TEST-SHARED", ProcessStatus.ACTIVE, auth.district()));
        var attendance = attendance(person, process, auth, "2026-10-06T15:00:00Z");
        attendance(homonym, process, auth, "2026-10-06T15:00:00Z");
        var receipt = em.persistAndFlush(new MediaAsset(UUID.randomUUID(), "receipt-key", "application/pdf", 3, MediaAssetKind.RECEIPT));
        attendance.completeReceipt(receipt);
        em.flush();
        em.clear();

        var events = events();
        assertThat(events).hasSize(3);
        assertThat(events).extracting(ConvictedScheduleEvent::id)
            .containsExactlyInAnyOrder(first.getUuid(), second.getUuid(), attendance.getUuid());
        assertThat(events).extracting(ConvictedScheduleEvent::date).containsOnly(day);
        var appearance = events.stream().filter(e -> e.origin() == Origin.INDIVIDUAL_APPEARANCE).findFirst().orElseThrow();
        assertThat(appearance.status()).isEqualTo(Status.COMPLETED);
        assertThat(appearance.receiptIssued()).isTrue();
        assertThat(appearance.processId()).isEqualTo(process.getUuid());
        assertThat(appearance.time()).isEqualTo(java.time.LocalTime.NOON);
        assertThat(events.stream().filter(e -> e.origin() == Origin.REFLECTION_GROUP))
            .allSatisfy(e -> {
                assertThat(e.status()).isEqualTo(Status.SCHEDULED);
                assertThat(e.participationStatus()).isNull();
                assertThat(e.processId()).isNull();
                assertThat(e.time()).isNull();
            });
    }

    @Test
    void readsRecordedPresenceAbsenceAndJustificationAfterLeavingTheGroup() {
        var group = group(auth, "Histórico", person, FROM, FROM.plusDays(10));
        for (var status : GroupPresenceStatus.values()) {
            var meeting = meeting(group, FROM.plusDays(status.ordinal()));
            var record = new GroupMeetingPresence(meeting, person, null, status,
                status == GroupPresenceStatus.JUSTIFICADO ? GroupJustificationType.values()[0] : null, null);
            meeting.markRealized(List.of(record));
        }
        // A recorded participation remains authoritative even outside a later edited membership period.
        var historic = meeting(group, FROM.plusDays(20));
        historic.markRealized(List.of(new GroupMeetingPresence(historic, person, null,
            GroupPresenceStatus.PRESENTE, null, null)));
        group.setConvicteds(List.of());
        em.flush();
        em.clear();

        assertThat(events()).hasSize(4).allSatisfy(e -> assertThat(e.status()).isEqualTo(Status.COMPLETED));
        assertThat(events()).extracting(ConvictedScheduleEvent::participationStatus)
            .containsExactly(GroupPresenceStatus.PRESENTE, GroupPresenceStatus.AUSENTE,
                GroupPresenceStatus.JUSTIFICADO, GroupPresenceStatus.PRESENTE);
    }

    @Test
    void appliesInclusiveMembershipDatesAndKeepsCancellationsAndRescheduling() {
        var group = group(auth, "Vínculo", person, FROM.plusDays(5), FROM.plusDays(15));
        meeting(group, FROM.plusDays(4));
        var start = meeting(group, FROM.plusDays(5));
        var cancelled = meeting(group, FROM.plusDays(8));
        cancelled.markNotRealized();
        var rescheduled = meeting(group, FROM.plusDays(9));
        rescheduled.updateSchedule(FROM.plusDays(12), "Tema remarcado");
        var end = meeting(group, FROM.plusDays(15));
        meeting(group, FROM.plusDays(16));
        em.flush();
        em.clear();

        var events = events();
        assertThat(events).extracting(ConvictedScheduleEvent::id)
            .containsExactly(start.getUuid(), cancelled.getUuid(), rescheduled.getUuid(), end.getUuid());
        assertThat(events.get(1).status()).isEqualTo(Status.CANCELLED);
        assertThat(events.get(2).date()).isEqualTo(FROM.plusDays(12));
    }

    @Test
    void doesNotInferAbsenceFromMissingAttendanceOrFromAPastPendingMeeting() {
        var group = group(auth, "Sem lançamento", person, FROM, null);
        meeting(group, FROM);
        meeting(group, FROM.plusDays(1)).markRealized(List.of());
        em.flush();
        em.clear();
        assertThat(events()).extracting(ConvictedScheduleEvent::status)
            .containsExactly(Status.SCHEDULED, Status.COMPLETED);
        assertThat(events()).allSatisfy(e -> assertThat(e.participationStatus()).isNull());
    }

    @Test
    void usesSaoPauloDayBoundariesAndDoesNotRequirePdfIssuance() {
        var group = group(auth, "Limites", person, FROM.minusDays(1), null);
        meeting(group, FROM.minusDays(1));
        var start = meeting(group, FROM);
        meeting(group, TO);
        var process = em.persistAndFlush(new JudicialProcess("TEST-BOUNDARY", ProcessStatus.INACTIVE, auth.district()));
        attendance(person, process, auth, "2026-10-01T02:59:59Z");
        var attendance = attendance(person, process, auth, "2026-10-01T03:00:00Z");
        attendance(person, process, auth, "2026-11-01T03:00:00Z");
        em.clear();

        assertThat(events()).extracting(ConvictedScheduleEvent::id)
            .containsExactly(attendance.getUuid(), start.getUuid());
        assertThat(events().getFirst().receiptIssued()).isFalse();
        assertThat(useCase.execute(person.getUuid(), TO, TO.plusDays(1), auth))
            .extracting(ConvictedScheduleEvent::date).containsOnly(TO);
        assertThatThrownBy(() -> useCase.execute(person.getUuid(), FROM, TO.plusDays(1), auth))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void isolatesDistrictEvenForInconsistentForeignGroupMembershipAndReturnsAnEmptyPeriod() {
        var foreignAuth = auth("Outra comarca", "12345678909");
        var foreignPerson = person(foreignAuth, "16899535009");
        meeting(group(foreignAuth, "Grupo externo", person, FROM, null), FROM);
        var foreignProcess = em.persistAndFlush(new JudicialProcess("FOREIGN", ProcessStatus.ACTIVE, foreignAuth.district()));
        attendance(person, foreignProcess, foreignAuth, "2026-10-01T12:00:00Z");
        em.clear();

        assertThat(events()).isEmpty();
        assertThatThrownBy(() -> useCase.execute(foreignPerson.getUuid(), FROM, TO, auth))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), FROM, TO, auth))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    private List<ConvictedScheduleEvent> events() {
        return useCase.execute(person.getUuid(), FROM, TO, auth);
    }

    private AuthContext auth(String name, String cpf) {
        var district = new JudicialDistrict();
        district.setName(name);
        em.persistAndFlush(district);
        var user = em.persistAndFlush(new User("Operador", Cpf.of(cpf), cpf + "@schedule.test", "hash", UserRole.OPERATOR, district));
        return new AuthContext(user, district, null);
    }

    private Convicted person(AuthContext context, String cpf) {
        return em.persistAndFlush(new Convicted("Homônimo", Cpf.of(cpf), LocalDate.of(1990, 1, 1),
            null, address, EmploymentStatus.UNEMPLOYED, context.district()));
    }

    private Group group(AuthContext context, String name, Convicted convicted, LocalDate start, LocalDate end) {
        var group = new Group();
        group.setName(name);
        group.setSubject("Tema");
        group.setDistrict(context.district());
        em.persistAndFlush(group);
        var membership = new GroupParticipantMembership(group, convicted, start);
        if (end != null) membership.endOn(end);
        em.persistAndFlush(membership);
        return group;
    }

    private GroupMeeting meeting(Group group, LocalDate date) {
        return em.persistAndFlush(new GroupMeeting(group, date, "Tema"));
    }

    private Attendance attendance(Convicted convicted, JudicialProcess process, AuthContext context, String date) {
        var photo = em.persistAndFlush(new MediaAsset(UUID.randomUUID(), UUID.randomUUID().toString(),
            "image/png", 3, MediaAssetKind.PHOTO));
        var attendance = em.persistAndFlush(new Attendance(convicted, process, context.district(), context.user(),
            address, null, EmploymentStatus.UNEMPLOYED, photo));
        em.getEntityManager().createNativeQuery("update attendance set created_at = :date where id = :id")
            .setParameter("date", Timestamp.from(Instant.parse(date))).setParameter("id", attendance.getId()).executeUpdate();
        return attendance;
    }
}
