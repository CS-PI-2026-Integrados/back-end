package br.com.sicape.api.application.attendance.usecase;

import java.time.Year;
import java.time.ZoneId;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.sicape.api.application.attendance.dto.AttendanceYearsResponse;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListAttendanceYearsUseCase {
    private final AttendanceRepository repository;

    @Transactional(readOnly = true)
    public AttendanceYearsResponse execute(AuthContext auth) {
        var zone = ZoneId.of("America/Sao_Paulo");
        int current = Year.now(zone).getValue();
        var bounds = repository.findPeriodBounds(auth.district());
        int first = bounds.getFirstCreatedAt() == null ? current
            : Math.min(current, bounds.getFirstCreatedAt().atZone(zone).getYear());
        int last = bounds.getLastCreatedAt() == null ? current
            : Math.max(current, bounds.getLastCreatedAt().atZone(zone).getYear());
        return new AttendanceYearsResponse(
            IntStream.rangeClosed(first, last).map(year -> last - (year - first)).boxed().toList());
    }
}
