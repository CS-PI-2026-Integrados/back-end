package br.com.sicape.api.application.convicted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.sicape.api.application.convicted.schedule.ConvictedScheduleEvent;
import br.com.sicape.api.application.convicted.schedule.ConvictedScheduleEvent.Origin;
import br.com.sicape.api.application.convicted.schedule.ConvictedScheduleEvent.Status;
import br.com.sicape.api.application.convicted.schedule.ConvictedScheduleSource;
import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.convicted.usecase.GetConvictedScheduleUseCase;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.exception.ScheduleUnavailableException;
import br.com.sicape.api.domain.exception.ValidationException;

class GetConvictedScheduleUseCaseTest {
    private static final LocalDate FROM = LocalDate.of(2026, 10, 1);
    private static final LocalDate TO = FROM.plusMonths(1);

    private final UUID id = UUID.randomUUID();
    private final AuthContext auth = new AuthContext(null, null, null);
    private final Convicted convicted = mock(Convicted.class);
    private final ConvictedFinder finder = mock(ConvictedFinder.class);
    private final ConvictedScheduleSource groups = mock(ConvictedScheduleSource.class);
    private final ConvictedScheduleSource individual = mock(ConvictedScheduleSource.class);
    private final GetConvictedScheduleUseCase useCase =
        new GetConvictedScheduleUseCase(finder, List.of(groups, individual));

    @BeforeEach
    void setUp() {
        when(finder.find(id, auth)).thenReturn(convicted);
        when(groups.origin()).thenReturn(Origin.REFLECTION_GROUP);
        when(individual.origin()).thenReturn(Origin.INDIVIDUAL_APPEARANCE);
    }

    @Test
    void combinesOccurrencesOnSameDayAndHonorsTheExclusiveEnd() {
        var start = event(Origin.REFLECTION_GROUP, FROM, "Grupo A", Status.SCHEDULED);
        var groupB = event(Origin.REFLECTION_GROUP, FROM.plusDays(21), "Grupo B", Status.SCHEDULED);
        var appearance = event(Origin.INDIVIDUAL_APPEARANCE, groupB.date(), "Comparecimento", Status.SCHEDULED);
        var cancelled = event(Origin.REFLECTION_GROUP, FROM.plusDays(29), "Grupo A", Status.CANCELLED);
        var nextMonth = event(Origin.REFLECTION_GROUP, TO, "Grupo A", Status.SCHEDULED);
        when(groups.findEvents(convicted, FROM, TO)).thenReturn(List.of(cancelled, nextMonth, groupB, start));
        when(individual.findEvents(convicted, FROM, TO)).thenReturn(List.of(appearance));

        assertThat(useCase.execute(id, FROM, TO, auth)).containsExactly(start, groupB, appearance, cancelled);
    }

    @Test
    void distinguishesEmptyMonthFromUnavailableSources() {
        when(groups.findEvents(convicted, FROM, TO)).thenReturn(List.of());
        when(individual.findEvents(convicted, FROM, TO)).thenReturn(List.of());
        assertThat(useCase.execute(id, FROM, TO, auth)).isEmpty();

        var partial = new GetConvictedScheduleUseCase(finder, List.of(groups));
        assertThatThrownBy(() -> partial.execute(id, FROM, TO, auth))
            .isInstanceOf(ScheduleUnavailableException.class);
    }

    @Test
    void rejectsInvalidPeriodAndAppliesDistrictLookup() {
        assertThatThrownBy(() -> useCase.execute(id, FROM, FROM, auth)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> useCase.execute(id, FROM, TO.plusDays(1), auth)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> useCase.execute(id, null, TO, auth)).isInstanceOf(ValidationException.class);

        var foreignId = UUID.randomUUID();
        when(finder.find(foreignId, auth)).thenThrow(new ResourceNotFoundException("Apenado não encontrado."));
        assertThatThrownBy(() -> useCase.execute(foreignId, FROM, TO, auth))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    private ConvictedScheduleEvent event(Origin origin, LocalDate date, String title, Status status) {
        return new ConvictedScheduleEvent(UUID.randomUUID(), origin, date, LocalTime.NOON, title, status, null, null, null);
    }
}
