package br.com.sicape.api.application.convicted;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.sicape.api.application.convicted.dto.request.ConvictedProcessRequest;
import br.com.sicape.api.application.convicted.dto.request.UpdateConvictedRequest;
import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.convicted.usecase.UpdateConvictedUseCase;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.JudicialProcess;
import br.com.sicape.api.domain.enums.ProcessStatus;
import br.com.sicape.api.domain.enums.ConvictedStatus;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.repository.JudicialProcessRepository;
import br.com.sicape.api.domain.valueobject.Address;
import br.com.sicape.api.domain.valueobject.Cpf;
import br.com.sicape.api.domain.valueobject.Phone;

class UpdateConvictedUseCaseTest {
    private final ConvictedFinder finder = mock(ConvictedFinder.class);
    private final ConvictedRepository repository = mock(ConvictedRepository.class);
    private final JudicialProcessRepository processRepository = mock(JudicialProcessRepository.class);
    private final JudicialDistrict district = new JudicialDistrict();
    private final AuthContext auth = new AuthContext(null, district, null);
    private UpdateConvictedUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateConvictedUseCase(finder, repository, processRepository);
    }

    @Test
    void rejectsInactiveProcessDuringUpdate() {
        UUID convictedId = UUID.randomUUID();
        UUID processId = UUID.randomUUID();
        Convicted convicted = new Convicted(
            "Apenado de Teste",
            Cpf.of("52998224725"),
            LocalDate.of(1990, 1, 1),
            Phone.of("11912345678"),
            new Address("12345678", "Rua Teste", "10", null, "Centro", "Cidade", "SP"),
            null,
            district
        );
        convicted.setUuid(convictedId);

        JudicialProcess inactiveProcess = new JudicialProcess(
            "0003456-78.2025.8.26.0001",
            ProcessStatus.INACTIVE,
            district
        );
        inactiveProcess.setUuid(processId);

        when(finder.findForUpdate(convictedId, auth)).thenReturn(convicted);
        when(processRepository.findAllByUuidInAndDistrict(any(), any()))
            .thenReturn(List.of(inactiveProcess));

        UpdateConvictedRequest request = new UpdateConvictedRequest();
        request.setProcesses(List.of(new ConvictedProcessRequest(processId, true)));

        assertThatThrownBy(() -> useCase.execute(convictedId, request, auth))
            .isInstanceOf(ValidationException.class)
            .satisfies(exception -> {
                ValidationException validation = (ValidationException) exception;
                org.assertj.core.api.Assertions.assertThat(validation.getFields())
                    .anySatisfy(field -> org.assertj.core.api.Assertions.assertThat(field.message())
                        .isEqualTo("Somente processos ativos podem ser vinculados"));
            });

        verify(repository, never()).save(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsInactiveConvictedEvenWithEmptyRequest() {
        var convicted = new Convicted("Apenado", Cpf.of("52998224725"), LocalDate.of(1990, 1, 1),
            Phone.of("11912345678"), new Address("12345678", "Rua", "10", null, "Centro", "Cidade", "SP"),
            null, district);
        convicted.updateStatus(ConvictedStatus.INACTIVE, null);
        UUID id = UUID.randomUUID();
        when(finder.findForUpdate(id, auth)).thenReturn(convicted);

        assertThatThrownBy(() -> useCase.execute(id, new UpdateConvictedRequest(), auth))
            .isInstanceOf(ConflictException.class).hasMessageContaining("Reative");
        verify(repository, never()).save(any());
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(processRepository);
    }
}
