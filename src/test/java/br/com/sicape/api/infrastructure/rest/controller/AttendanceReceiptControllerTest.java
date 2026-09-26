package br.com.sicape.api.infrastructure.rest.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.sicape.api.application.attendance.usecase.GetAttendanceReceiptUseCase;
import br.com.sicape.api.application.common.media.MediaContent;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.exception.ValidationException;

class AttendanceReceiptControllerTest {
    private final GetAttendanceReceiptUseCase useCase = mock(GetAttendanceReceiptUseCase.class);
    private final AttendanceController controller = new AttendanceController(null, null, useCase);
    private final UUID id = UUID.randomUUID();
    private final AuthContext auth = mock(AuthContext.class);

    @Test
    void sendsInlineDisposition() {
        when(useCase.execute(id, auth)).thenReturn(new MediaContent(new byte[]{1}, "application/pdf"));
        var response = controller.getReceipt(id, "inline", auth);
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
            .startsWith("inline;").contains("comprovante-" + id + ".pdf");
    }

    @Test
    void sendsAttachmentDisposition() {
        when(useCase.execute(id, auth)).thenReturn(new MediaContent(new byte[]{1}, "application/pdf"));
        var response = controller.getReceipt(id, "attachment", auth);
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
            .startsWith("attachment;").contains("comprovante-" + id + ".pdf");
    }

    @Test
    void rejectsUnknownDisposition() {
        assertThatThrownBy(() -> controller.getReceipt(id, "other", auth))
            .isInstanceOf(ValidationException.class);
        verifyNoInteractions(useCase);
    }
}
