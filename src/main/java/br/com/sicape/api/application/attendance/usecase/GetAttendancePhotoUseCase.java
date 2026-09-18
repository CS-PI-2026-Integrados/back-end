package br.com.sicape.api.application.attendance.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.common.media.MediaAssetStore;
import br.com.sicape.api.application.common.media.MediaContent;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAttendancePhotoUseCase {
    private final AttendanceRepository attendances;
    private final MediaAssetStore media;

    @Transactional(readOnly = true)
    public MediaContent execute(UUID uuid, AuthContext auth) {
        var attendance = attendances.findByUuidAndDistrict(uuid, auth.district())
            .orElseThrow(() -> new ResourceNotFoundException("Presença não encontrada."));

        if (attendance.getPhoto() == null) {
            throw new ResourceNotFoundException("Foto não encontrada.");
        }
        return media.read(attendance.getPhoto());
    }
}
