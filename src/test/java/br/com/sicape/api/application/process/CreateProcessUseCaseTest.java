package br.com.sicape.api.application.process;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.application.process.dto.request.CreateProcessRequest;
import br.com.sicape.api.application.process.usecase.CreateProcessUseCase;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.JudicialProcess;
import br.com.sicape.api.domain.enums.ProcessStatus;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.repository.JudicialProcessRepository;

class CreateProcessUseCaseTest {
    private final JudicialProcessRepository repository = mock(JudicialProcessRepository.class);
    private final CreateProcessUseCase useCase = new CreateProcessUseCase(repository);
    private final AuthContext auth = new AuthContext(null, new JudicialDistrict(), null);

    @ParameterizedTest
    @ValueSource(strings = {"0001234-56.2026.8.26.0001", "00012345620268260001", " 0001234-56.2026.8.26.0001 "})
    void createsCanonicalActiveProcessInAuthenticatedDistrict(String number) {
        when(repository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        var result = useCase.execute(new CreateProcessRequest(number), auth);
        var captor = org.mockito.ArgumentCaptor.forClass(JudicialProcess.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getDistrict()).isSameAs(auth.district());
        assertThat(captor.getValue().getNormalizedNumber()).isEqualTo("00012345620268260001");
        assertThat(result.number()).isEqualTo("0001234-56.2026.8.26.0001");
        assertThat(result.status()).isEqualTo(ProcessStatus.ACTIVE);
        assertThat(result.linkedConvictedCount()).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "123", "0001234-56x2026x8x26x0001", "0001234-56.2026.8.26.0001 extra"})
    void rejectsInvalidNumbersBeforeQuerying(String number) {
        assertThatThrownBy(() -> useCase.execute(new CreateProcessRequest(number), auth))
            .isInstanceOf(ValidationException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsExistingNormalizedNumberRegardlessOfFormattingStatusOrDistrict() {
        when(repository.existsByNormalizedNumber("00012345620268260001")).thenReturn(true);
        assertThatThrownBy(() -> useCase.execute(new CreateProcessRequest("00012345620268260001"), auth))
            .isInstanceOf(ConflictException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void translatesConcurrentUniqueConstraintViolation() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique number"));
        assertThatThrownBy(() -> useCase.execute(new CreateProcessRequest("00012345620268260001"), auth))
            .isInstanceOf(ConflictException.class);
    }
}
