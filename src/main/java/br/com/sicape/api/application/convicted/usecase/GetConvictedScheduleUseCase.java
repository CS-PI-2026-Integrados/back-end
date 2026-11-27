package br.com.sicape.api.application.convicted.usecase;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.convicted.schedule.ConvictedScheduleEvent;
import br.com.sicape.api.application.convicted.schedule.ConvictedScheduleEvent.Origin;
import br.com.sicape.api.application.convicted.schedule.ConvictedScheduleSource;
import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.exception.ScheduleUnavailableException;
import br.com.sicape.api.domain.exception.ValidationException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetConvictedScheduleUseCase {
    private static final Comparator<ConvictedScheduleEvent> EVENT_ORDER = Comparator
        .comparing(ConvictedScheduleEvent::date)
        .thenComparing(ConvictedScheduleEvent::time, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(ConvictedScheduleEvent::origin)
        .thenComparing(ConvictedScheduleEvent::id);

    private final ConvictedFinder finder;
    private final List<ConvictedScheduleSource> sources;

    @Transactional(readOnly = true)
    public List<ConvictedScheduleEvent> execute(UUID uuid, LocalDate from, LocalDate to, AuthContext auth) {
        if (from == null) {
            throw new ValidationException("from", "Informe a data inicial.");
        }
        if (to == null || !to.isAfter(from) || ChronoUnit.DAYS.between(from, to) > 31) {
            throw new ValidationException("to", "Informe uma data final posterior ao início, em um intervalo de até 31 dias.");
        }

        var convicted = finder.find(uuid, auth);
        var unavailable = EnumSet.allOf(Origin.class);
        sources.forEach(source -> unavailable.remove(source.origin()));
        if (!unavailable.isEmpty()) {
            throw new ScheduleUnavailableException();
        }

        return sources.stream()
            .flatMap(source -> source.findEvents(convicted, from, to).stream())
            .filter(event -> !event.date().isBefore(from) && event.date().isBefore(to))
            .sorted(EVENT_ORDER)
            .toList();
    }
}
