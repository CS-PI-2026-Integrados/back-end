package br.com.sicape.api.application.attendance.usecase;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.sicape.api.application.attendance.dto.AttendanceMonthsResponse;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListAttendanceMonthsUseCase {
    private final AttendanceRepository repository;

    @Transactional(readOnly = true)
    public AttendanceMonthsResponse execute(int year, AuthContext auth) {
        if (year < 1 || year > 9998) {
            throw new ValidationException("year", "Informe um ano entre 1 e 9998");
        }
        var zone = ZoneId.of("America/Sao_Paulo");
        return new AttendanceMonthsResponse(IntStream.rangeClosed(1, 12).mapToObj(month -> {
            var start = LocalDate.of(year, month, 1);
            return repository.countByDistrictAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                auth.district(), start.atStartOfDay(zone).toInstant(),
                start.plusMonths(1).atStartOfDay(zone).toInstant());
        }).toList());
    }
}
