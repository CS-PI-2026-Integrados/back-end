package br.com.sicape.api.application.attendance.usecase;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.sicape.api.application.attendance.dto.AttendanceMetricsResponse;
import br.com.sicape.api.application.attendance.dto.AttendanceMetricsResponse.MonthlyCount;
import br.com.sicape.api.application.attendance.dto.AttendanceMetricsResponse.RecentActivity;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAttendanceMetricsUseCase {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private final AttendanceRepository repository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AttendanceMetricsResponse execute(AuthContext auth) {
        var now = clock.instant();
        long recent = repository.countByDistrictAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            auth.district(), now.minus(7, ChronoUnit.DAYS), now);
        var currentMonth = YearMonth.from(now.atZone(ZONE));
        var months = new ArrayList<MonthlyCount>(6);
        for (int offset = 5; offset >= 0; offset--) {
            var month = currentMonth.minusMonths(offset);
            var start = month.atDay(1).atStartOfDay(ZONE).toInstant();
            var end = offset == 0 ? now : month.plusMonths(1).atDay(1).atStartOfDay(ZONE).toInstant();
            months.add(new MonthlyCount(month.toString(),
                repository.countByDistrictAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    auth.district(), start, end)));
        }
        var activities = repository.findRecentActivities(auth.district(), now, PageRequest.of(0, 4))
            .stream().map(item -> new RecentActivity(
                item.id(), item.convictedId(), item.convictedName(), item.createdAt())).toList();
        return new AttendanceMetricsResponse(recent, months, activities, now);
    }
}
