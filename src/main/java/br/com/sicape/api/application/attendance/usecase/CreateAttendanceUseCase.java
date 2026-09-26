package br.com.sicape.api.application.attendance.usecase;

import org.springframework.stereotype.Service;
import br.com.sicape.api.application.attendance.dto.AttendanceResponse;
import br.com.sicape.api.application.attendance.dto.CreateAttendanceRequest;
import br.com.sicape.api.application.common.media.MediaAssetStore;
import br.com.sicape.api.application.common.validation.PhotoValidator;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Attendance;
import br.com.sicape.api.domain.entity.MediaAsset;
import br.com.sicape.api.domain.enums.ConvictedStatus;
import br.com.sicape.api.domain.enums.MediaAssetKind;
import br.com.sicape.api.domain.enums.ProcessStatus;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.repository.*;
import br.com.sicape.api.domain.valueobject.Address;
import br.com.sicape.api.domain.valueobject.Phone;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateAttendanceUseCase {
    private final ConvictedRepository convicteds;
    private final JudicialProcessRepository processes;
    private final ConvictedProcessRepository links;
    private final AttendanceRepository attendances;
    private final MediaAssetStore media;
    private final PhotoValidator photoValidator;

    @Transactional
    public AttendanceResponse execute(
        CreateAttendanceRequest request,
        byte[] content,
        String declaredType,
        AuthContext auth
    ) {
        Address address;
        try {
            address = request.address().toValueObject();
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("address", exception.getMessage());
        }
        Phone phone;
        try {
            phone = Phone.of(request.phone());
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("phone", exception.getMessage());
        }
        String contentType = photoValidator.validate(content, declaredType);

        var convicted = convicteds
            .findForAttendance(request.convictedId(), auth.district(), ConvictedStatus.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException("Apenado ativo não encontrado."));
        var process = processes
            .findByUuidAndDistrictAndStatus(request.processId(), auth.district(), ProcessStatus.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException("Processo ativo não encontrado."));

        if (!links.existsByConvictedAndProcess(convicted, process)) {
            throw new ValidationException("process_id", "O processo não está vinculado ao apenado informado");
        }

        MediaAsset savedPhoto = media.save(content, contentType, MediaAssetKind.PHOTO);

        var attendance = new Attendance(
            convicted,
            process,
            auth.district(),
            auth.user(),
            address,
            phone,
            request.employmentStatus(),
            savedPhoto
        );
        convicted.updateAddress(address);
        convicted.updatePhone(phone);
        convicted.updateEmploymentStatus(request.employmentStatus());

        return AttendanceResponse.from(attendances.saveAndFlush(attendance));
    }
}
