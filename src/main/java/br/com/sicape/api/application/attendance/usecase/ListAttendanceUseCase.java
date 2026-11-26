package br.com.sicape.api.application.attendance.usecase;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
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
        return execute(search, year, month, null, null, page, size, auth, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> execute(
        String search, Integer year, Integer month, int page, int size, AuthContext auth, UUID convictedId
    ) {
        return execute(search, year, month, null, null, page, size, auth, convictedId);
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> execute(
        String search,
        Integer year,
        Integer month,
        LocalDate startDate,
        LocalDate endDate,
        int page,
        int size,
        AuthContext auth
    ) {
        return execute(search, year, month, startDate, endDate, page, size, auth, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> execute(
        String search,
        Integer year,
        Integer month,
        LocalDate startDate,
        LocalDate endDate,
        int page,
        int size,
        AuthContext auth,
        UUID convictedId
    ) {
        if (year != null && (year < 1 || year > 9998)) {
            throw new ValidationException("year", "Informe um ano entre 1 e 9998");
        }
        if (month != null && (month < 1 || month > 12 || year == null)) {
            throw new ValidationException("month", "Informe um mês entre 1 e 12 e selecione um ano");
        }
        validateDate("startDate", startDate);
        validateDate("endDate", endDate);
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ValidationException("startDate", "A data inicial não pode ser posterior à data final");
        }

        var zone = ZoneId.of("America/Sao_Paulo");
        var periodStartDate = year == null ? null : LocalDate.of(year, month == null ? 1 : month, 1);
        var periodEndDate = periodStartDate == null ? null
            : month == null ? periodStartDate.plusYears(1) : periodStartDate.plusMonths(1);
        var effectiveStartDate = periodStartDate;
        if (startDate != null && (effectiveStartDate == null || startDate.isAfter(effectiveStartDate))) {
            effectiveStartDate = startDate;
        }
        var effectiveEndDate = periodEndDate;
        if (endDate != null) {
            var exclusiveEndDate = endDate.plusDays(1);
            if (effectiveEndDate == null || exclusiveEndDate.isBefore(effectiveEndDate)) {
                effectiveEndDate = exclusiveEndDate;
            }
        }

        search = search == null ? "" : search.trim().toLowerCase();
        String digits = search.replaceAll("\\D", "");

        var result = repository.search(
            auth.district(),
            convictedId,
            search,
            digits,
            effectiveStartDate == null ? null : effectiveStartDate.atStartOfDay(zone).toInstant(),
            effectiveEndDate == null ? null : effectiveEndDate.atStartOfDay(zone).toInstant(),
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

    private void validateDate(String field, LocalDate date) {
        if (date != null && (date.getYear() < 1 || date.getYear() > 9998)) {
            throw new ValidationException(field, "Informe uma data entre os anos 1 e 9998");
        }
    }
}
