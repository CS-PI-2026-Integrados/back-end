package br.com.sicape.api.application.convicted.usecase;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.sicape.api.application.convicted.dto.response.ConvictedMetricsResponse;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.enums.ConvictedStatus;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetConvictedMetricsUseCase {
    private final ConvictedRepository repository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ConvictedMetricsResponse execute(AuthContext auth) {
        var now = clock.instant();
        long total = repository.countByDistrict(auth.district());
        long active = repository.countByDistrictAndStatus(auth.district(), ConvictedStatus.ACTIVE);
        long recent = repository.countWithRecentAttendance(
            auth.district(), ConvictedStatus.ACTIVE, now.minus(30, ChronoUnit.DAYS), now);
        return new ConvictedMetricsResponse(total, active, total - active, recent, active - recent, now);
    }
}
