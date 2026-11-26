package br.com.sicape.api.application.convicted.schedule;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Component;

import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AttendanceConvictedScheduleSource implements ConvictedScheduleSource {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private final AttendanceRepository attendances;

    @Override
    public ConvictedScheduleEvent.Origin origin() {
        return ConvictedScheduleEvent.Origin.INDIVIDUAL_APPEARANCE;
    }

    @Override
    public List<ConvictedScheduleEvent> findEvents(Convicted convicted, LocalDate from, LocalDate to) {
        return attendances.findSchedule(convicted.getUuid(), convicted.getDistrict(),
            from.atStartOfDay(BUSINESS_ZONE).toInstant(), to.atStartOfDay(BUSINESS_ZONE).toInstant())
            .stream().map(attendance -> {
                var dateTime = attendance.attendedAt().atZone(BUSINESS_ZONE);
                return new ConvictedScheduleEvent(
                    attendance.id(), origin(), dateTime.toLocalDate(), dateTime.toLocalTime(),
                    "Comparecimento individual", ConvictedScheduleEvent.Status.COMPLETED,
                    attendance.processId(), null, attendance.receiptIssued()
                );
            }).toList();
    }
}
