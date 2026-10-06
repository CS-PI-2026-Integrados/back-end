package br.com.sicape.api.application.attendance.usecase;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.attendance.dto.AttendanceResponse;
import br.com.sicape.api.application.common.dto.response.PageResponse;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.repository.AttendanceRepository;
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
        search = search == null ? "" : search.trim().toLowerCase();
        String digits = search.replaceAll("\\D", "");

        var result = repository.search(
            auth.district(),
            search,
            digits,
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
