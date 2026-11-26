package br.com.sicape.api.application.convicted.schedule;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.repository.GroupMeetingRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GroupConvictedScheduleSource implements ConvictedScheduleSource {
    private final GroupMeetingRepository meetings;

    @Override
    public ConvictedScheduleEvent.Origin origin() {
        return ConvictedScheduleEvent.Origin.REFLECTION_GROUP;
    }

    @Override
    public List<ConvictedScheduleEvent> findEvents(Convicted convicted, LocalDate from, LocalDate to) {
        return meetings.findSchedule(convicted, convicted.getDistrict(), from, to).stream()
            .map(meeting -> new ConvictedScheduleEvent(
                meeting.id(), origin(), meeting.date(), null, meeting.title(),
                switch (meeting.status()) {
                    case PENDENTE -> ConvictedScheduleEvent.Status.SCHEDULED;
                    case REALIZADO -> ConvictedScheduleEvent.Status.COMPLETED;
                    case CANCELADO -> ConvictedScheduleEvent.Status.CANCELLED;
                },
                null, meeting.participationStatus(), null
            ))
            .toList();
    }
}
