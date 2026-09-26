package br.com.sicape.api.application.attendance;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.sicape.api.application.attendance.receipt.*;
import br.com.sicape.api.application.attendance.usecase.GetAttendanceReceiptUseCase;
import br.com.sicape.api.application.common.media.MediaAssetStore;
import br.com.sicape.api.application.common.media.MediaContent;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.*;
import br.com.sicape.api.domain.enums.*;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.repository.AttendanceRepository;
import br.com.sicape.api.domain.valueobject.*;

class GetAttendanceReceiptUseCaseTest {
    private final AttendanceRepository attendances = mock(AttendanceRepository.class);
    private final MediaAssetStore media = mock(MediaAssetStore.class);
    private final ReceiptPdfRenderer renderer = mock(ReceiptPdfRenderer.class);
    private final GetAttendanceReceiptUseCase useCase = new GetAttendanceReceiptUseCase(
        attendances, media, renderer, new ObjectMapper().findAndRegisterModules());

    private Attendance attendance;
    private AuthContext auth;
    private MediaAsset photo;
    private MediaAsset convictedPhoto;
    private Convicted convicted;
    private final byte[] pdf = "%PDF-1.7 test".getBytes();

    @BeforeEach
    void setUp() {
        var district = new JudicialDistrict();
        district.setUuid(UUID.randomUUID());
        district.setName("Comarca Central");
        var user = new User();
        user.setName("Operador");
        user.setDistrict(district);
        auth = new AuthContext(user, district, null);
        convicted = mock(Convicted.class);
        when(convicted.getName()).thenReturn("Arthur Morgan");
        when(convicted.getCpf()).thenReturn(Cpf.of("52998224725"));
        var process = new JudicialProcess("0001234-56.2026.8.26.0001", ProcessStatus.ACTIVE, district);
        photo = new MediaAsset(UUID.randomUUID(), UUID.randomUUID().toString(),
            "image/png", 3, MediaAssetKind.PHOTO);
        convictedPhoto = new MediaAsset(UUID.randomUUID(), UUID.randomUUID().toString(),
            "image/png", 3, MediaAssetKind.PHOTO);
        when(convicted.getPhoto()).thenReturn(convictedPhoto);
        attendance = new Attendance(convicted, process, district, user,
            new Address("12345678", "Rua Central", "10", null, "Centro", "Cidade", "SP"),
            Phone.of("11988881001"), EmploymentStatus.FORMAL_WORK, photo);
        attendance.setUuid(UUID.randomUUID());
        ReflectionTestUtils.setField(attendance, "createdAt", Instant.parse("2026-09-25T22:42:00Z"));
        when(attendances.findForReceipt(attendance.getUuid(), district)).thenReturn(Optional.of(attendance));
        when(media.read(photo)).thenReturn(new MediaContent(new byte[]{1, 2, 3}, "image/png"));
        when(media.read(convictedPhoto)).thenReturn(new MediaContent(new byte[]{4, 5, 6}, "image/png"));
        when(renderer.template()).thenReturn("<html>template</html>");
        when(renderer.logoSha256()).thenReturn("logo-hash");
        when(renderer.render(any(), any(), any())).thenReturn(pdf);
    }

    @Test
    void generatesOncePersistsReceiptAndReusesSavedFile() {
        var receipt = new MediaAsset(UUID.randomUUID(), UUID.randomUUID().toString(),
            "application/pdf", pdf.length, MediaAssetKind.RECEIPT);
        when(media.save(pdf, "application/pdf", MediaAssetKind.RECEIPT)).thenReturn(receipt);
        when(media.read(receipt)).thenReturn(new MediaContent(pdf, "application/pdf"));

        var first = useCase.execute(attendance.getUuid(), auth);
        var second = useCase.execute(attendance.getUuid(), auth);

        assertThat(first.bytes()).isEqualTo(pdf);
        assertThat(second.bytes()).isEqualTo(pdf);
        assertThat(attendance.getReceipt()).isSameAs(receipt);
        var snapshotCaptor = ArgumentCaptor.forClass(ReceiptSnapshot.class);
        verify(renderer, times(1)).render(snapshotCaptor.capture(), any(), any());
        var snapshot = snapshotCaptor.getValue();
        assertThat(snapshot.attendanceId()).isEqualTo(attendance.getUuid());
        assertThat(snapshot.templateXhtml()).isEqualTo("<html>template</html>");
        assertThat(snapshot.logoSha256()).isEqualTo("logo-hash");
        assertThat(snapshot.address()).isEqualTo("Rua Central, 10 - Centro, Cidade/SP - CEP 12345678");
        assertThat(snapshot.employmentStatus()).isEqualTo("Trabalho formal");
        assertThat(snapshot.convictedCpf()).isEqualTo("529.982.247-25");
        assertThat(snapshot.phone()).isEqualTo("(11) 98888-1001");
        verify(media, times(1)).save(pdf, "application/pdf", MediaAssetKind.RECEIPT);
        verify(attendances, times(1)).saveAndFlush(attendance);
        verify(attendances, times(2)).findForReceipt(attendance.getUuid(), auth.district());
    }

    @Test
    void missingAttendanceInDistrictReturns404() {
        when(attendances.findForReceipt(any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute(attendance.getUuid(), auth))
            .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(media, renderer);
    }

    @Test
    void missingAttendancePhotoBlocksGeneration() {
        when(media.read(photo)).thenThrow(new ResourceNotFoundException("Arquivo não encontrado."));
        assertThatThrownBy(() -> useCase.execute(attendance.getUuid(), auth))
            .isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(renderer);
        verify(media, never()).save(any(), any(), any());
        assertThat(attendance.getReceipt()).isNull();
    }

    @Test
    void missingConvictedPhotoBlocksGenerationBeforeReadingStorage() {
        when(convicted.getPhoto()).thenReturn(null);
        assertThatThrownBy(() -> useCase.execute(attendance.getUuid(), auth))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("foto de cadastro");
        verify(media, never()).read(any());
        verify(media, never()).save(any(), any(), any());
        verifyNoInteractions(renderer);
        assertThat(attendance.getReceipt()).isNull();
    }

    @Test
    void missingConvictedPhotoFileBlocksGeneration() {
        when(media.read(convictedPhoto)).thenThrow(new ResourceNotFoundException("Arquivo não encontrado."));
        assertThatThrownBy(() -> useCase.execute(attendance.getUuid(), auth))
            .isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(ResourceNotFoundException.class);
        verify(renderer, never()).render(any(), any(), any());
        verify(media, never()).save(any(), any(), any());
        assertThat(attendance.getReceipt()).isNull();
    }

    @Test
    void rendererFailureBlocksGeneration() {
        when(renderer.render(any(), any(), any())).thenThrow(new IllegalStateException("Falha ao renderizar"));
        assertThatThrownBy(() -> useCase.execute(attendance.getUuid(), auth))
            .isInstanceOf(IllegalStateException.class);
        verify(media, never()).save(any(), any(), any());
        assertThat(attendance.getReceipt()).isNull();
    }

    @Test
    void missingPersistedReceiptDoesNotRegenerate() {
        var receipt = new MediaAsset(UUID.randomUUID(), UUID.randomUUID().toString(),
            "application/pdf", pdf.length, MediaAssetKind.RECEIPT);
        attendance.completeReceipt(receipt);
        when(media.read(receipt)).thenThrow(new ResourceNotFoundException("Arquivo não encontrado."));
        assertThatThrownBy(() -> useCase.execute(attendance.getUuid(), auth))
            .isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(renderer);
        verify(media, never()).read(photo);
        verify(media, never()).read(convictedPhoto);
        verify(media, never()).save(any(), any(), any());
    }
}
