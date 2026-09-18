package br.com.sicape.api.application.attendance;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import br.com.sicape.api.application.attendance.dto.CreateAttendanceRequest;
import br.com.sicape.api.application.attendance.usecase.CreateAttendanceUseCase;
import br.com.sicape.api.application.common.dto.request.AddressRequest;
import br.com.sicape.api.application.common.media.MediaAssetStore;
import br.com.sicape.api.application.common.validation.PhotoValidator;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.*;
import br.com.sicape.api.domain.enums.*;
import br.com.sicape.api.domain.exception.*;
import br.com.sicape.api.domain.repository.*;
import br.com.sicape.api.domain.valueobject.Phone;

class CreateAttendanceUseCaseTest {
    private final ConvictedRepository convicteds = mock(ConvictedRepository.class);
    private final JudicialProcessRepository processes = mock(JudicialProcessRepository.class);
    private final ConvictedProcessRepository links = mock(ConvictedProcessRepository.class);
    private final AttendanceRepository attendances = mock(AttendanceRepository.class);
    private final MediaAssetStore media = mock(MediaAssetStore.class);
    private final Convicted convicted = mock(Convicted.class);
    private final JudicialProcess process = mock(JudicialProcess.class);
    private final MediaAsset photo = new MediaAsset(UUID.randomUUID(), UUID.randomUUID().toString(),
        "image/jpeg", 3, MediaAssetKind.PHOTO);
    private CreateAttendanceUseCase useCase;
    private AuthContext auth;
    private CreateAttendanceRequest request;

    @BeforeEach
    void setUp() {
        var district = new JudicialDistrict();
        district.setUuid(UUID.randomUUID());
        var user = new User();
        user.setUuid(UUID.randomUUID());
        user.setName("Operador original");
        user.setDistrict(district);
        auth = new AuthContext(user, district, null);
        request = new CreateAttendanceRequest(UUID.randomUUID(), UUID.randomUUID(),
            new AddressRequest("12345-678", "Rua", "10", null, "Centro", "Cidade", "SP"),
            "(11) 91234-5678", EmploymentStatus.FORMAL_WORK);
        when(convicteds.findForAttendance(request.convictedId(), district, ConvictedStatus.ACTIVE))
            .thenReturn(Optional.of(convicted));
        when(processes.findByUuidAndDistrictAndStatus(request.processId(), district, ProcessStatus.ACTIVE))
            .thenReturn(Optional.of(process));
        when(links.existsByConvictedAndProcess(convicted, process)).thenReturn(true);
        when(media.save(any(), anyString(), eq(MediaAssetKind.PHOTO))).thenReturn(photo);
        when(attendances.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        useCase = new CreateAttendanceUseCase(convicteds, processes, links, attendances, media,
            new PhotoValidator());
    }

    private void execute() {
        useCase.execute(request, AttendanceValidationTest.jpeg(3), "image/jpeg", auth);
    }

    @Test
    void createsAttendanceAndUpdatesOnlyContactData() {
        execute();
        var captor = ArgumentCaptor.forClass(Attendance.class);
        verify(attendances).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(auth.user());
        assertThat(captor.getValue().getPhoto()).isSameAs(photo);
        verify(convicteds).findForAttendance(request.convictedId(), auth.district(), ConvictedStatus.ACTIVE);
        verify(convicted).updateAddress(any());
        verify(convicted).updatePhone(Phone.of("(11) 91234-5678"));
        verify(convicted).updateEmploymentStatus(EmploymentStatus.FORMAL_WORK);
        verify(convicted, never()).completePhoto(any());
        verify(media, never()).deleteContent(any());
    }

    @Test
    void rejectsMissingConvictedBeforeWritingPhoto() {
        when(convicteds.findForAttendance(any(), any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(this::execute).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(media);
    }

    @Test
    void rejectsMissingProcessBeforeWritingPhoto() {
        when(processes.findByUuidAndDistrictAndStatus(any(), any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(this::execute).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(media);
    }

    @Test
    void rejectsMissingLinkBeforeWritingPhoto() {
        when(links.existsByConvictedAndProcess(any(), any())).thenReturn(false);
        assertThatThrownBy(this::execute).isInstanceOf(ValidationException.class);
        verifyNoInteractions(media);
    }

    @Test
    void rollsBackWhenPhotoSaveFailsWithoutDeletingUnownedKey() {
        var failure = new IllegalStateException("storage failed");
        doThrow(failure).when(media).save(any(), any(), any());
        assertThatThrownBy(this::execute).isSameAs(failure);
        verify(media, never()).deleteContent(any());
        verifyNoInteractions(attendances);
    }

    @Test
    void propagatesFlushFailureForStoreToCompensateOnRollback() {
        var failure = new IllegalStateException("flush failed");
        when(attendances.saveAndFlush(any())).thenThrow(failure);

        assertThatThrownBy(this::execute).isSameAs(failure);
        verify(media).save(any(), any(), eq(MediaAssetKind.PHOTO));
        verify(media, never()).deleteContent(any());
    }

    @Test
    void rejectsInvalidPhotoBeforeQueryingReferences() {
        assertThatThrownBy(() -> useCase.execute(request, new byte[0], "image/jpeg", auth))
            .isInstanceOf(ValidationException.class);
        verifyNoInteractions(convicteds, processes, links, attendances, media);
    }

    @Test
    void rejectsInvalidPhoneBeforeQueryingReferences() {
        request = new CreateAttendanceRequest(
            request.convictedId(),
            request.processId(),
            request.address(),
            "91234-5678",
            request.employmentStatus()
        );

        assertThatExceptionOfType(ValidationException.class)
            .isThrownBy(this::execute)
            .satisfies(exception -> assertThat(exception.getFields())
                .extracting(FieldViolation::field)
                .containsExactly("phone"));
        verifyNoInteractions(convicteds, processes, links, attendances, media);
    }

}
