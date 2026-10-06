package br.com.sicape.api.application.attendance.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.attendance.dto.AttendanceResponse;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAttendanceUseCase {
    private final AttendanceRepository repository;

    @Transactional(readOnly = true)
    public AttendanceResponse execute(UUID uuid, AuthContext auth) {
        var attendance = repository.findByUuidAndDistrict(uuid, auth.district())
            .orElseThrow(() -> new ResourceNotFoundException("Presença não encontrada."));
        return AttendanceResponse.from(attendance);
    }
}
