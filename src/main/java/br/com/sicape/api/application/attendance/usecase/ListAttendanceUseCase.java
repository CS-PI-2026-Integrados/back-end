package br.com.sicape.api.application.attendance.usecase;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.attendance.dto.AttendanceResponse;
import br.com.sicape.api.application.common.dto.response.PageResponse;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.repository.AttendanceRepository;
import br.com.sicape.api.domain.exception.ValidationException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListAttendanceUseCase {
    private final AttendanceRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> execute(
        String search,
        int page,
        int size,
        AuthContext auth
    ) {
        return execute(search, null, null, page, size, auth);
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> execute(
        String search, Integer year, Integer month, int page, int size, AuthContext auth
    ) {
        if (year != null && (year < 1 || year > 9998)) {
            throw new ValidationException("year", "Informe um ano entre 1 e 9998");
        }
        if (month != null && (month < 1 || month > 12 || year == null)) {
            throw new ValidationException("month", "Informe um mês entre 1 e 12 e selecione um ano");
        }
        var zone = ZoneId.of("America/Sao_Paulo");
        var startDate = year == null ? null : LocalDate.of(year, month == null ? 1 : month, 1);
        var endDate = startDate == null ? null
            : month == null ? startDate.plusYears(1) : startDate.plusMonths(1);
        search = search == null ? "" : search.trim().toLowerCase();
        String digits = search.replaceAll("\\D", "");

        var result = repository.search(
            auth.district(),
            search,
            digits,
            startDate == null ? null : startDate.atStartOfDay(zone).toInstant(),
            endDate == null ? null : endDate.atStartOfDay(zone).toInstant(),
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );

        return new PageResponse<>(
            result.getContent().stream().map(AttendanceResponse::from).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }
}
