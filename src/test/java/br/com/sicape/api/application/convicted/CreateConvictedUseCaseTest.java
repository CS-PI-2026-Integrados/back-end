package br.com.sicape.api.application.convicted;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.sicape.api.application.convicted.dto.request.ConvictedProcessRequest;
import br.com.sicape.api.application.convicted.dto.request.CreateConvictedRequest;
import br.com.sicape.api.application.convicted.usecase.CreateConvictedUseCase;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.JudicialProcess;
import br.com.sicape.api.domain.enums.ProcessStatus;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.repository.JudicialProcessRepository;
import br.com.sicape.api.domain.valueobject.Address;

class CreateConvictedUseCaseTest {
    private final ConvictedRepository repository = mock(ConvictedRepository.class);
    private final JudicialProcessRepository processRepository = mock(JudicialProcessRepository.class);
    private final JudicialDistrict district = new JudicialDistrict();
    private final AuthContext auth = new AuthContext(null, district, null);
    private CreateConvictedUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateConvictedUseCase(repository, processRepository);
        when(repository.existsByCpf(any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsConvictedWithAnActivePrincipalProcess() {
        UUID processId = UUID.randomUUID();
        JudicialProcess process = new JudicialProcess(
            "0001234-56.2026.8.26.0001",
            ProcessStatus.ACTIVE,
            district
        );
        process.setUuid(processId);
        when(processRepository.findAllByUuidInAndDistrict(any(), any())).thenReturn(List.of(process));

        useCase.execute(request(List.of(new ConvictedProcessRequest(processId, true))), auth);

        verify(repository).save(any());
    }

    @Test
    void createsFifteenConvictedsWithUniqueActivePrincipalProcesses() {
        for (int index = 0; index < 15; index++) {
            UUID processId = UUID.randomUUID();
            JudicialProcess process = new JudicialProcess(
                "000" + (1234000 + index) + "-" + (56 + index) + ".2026.8.26.0001",
                ProcessStatus.ACTIVE,
                district
            );
            process.setUuid(processId);
            when(processRepository.findAllByUuidInAndDistrict(any(), any()))
                .thenReturn(List.of(process));

            var response = useCase.execute(
                requestWithCpf(cpfForIndex(index), "Apenado de Teste " + index,
                    List.of(new ConvictedProcessRequest(processId, true))),
                auth
            );

            assertThat(response.name()).isEqualTo("Apenado de Teste " + index);
        }

        verify(repository, org.mockito.Mockito.times(15)).save(any());
        verify(processRepository, org.mockito.Mockito.times(15))
            .findAllByUuidInAndDistrict(any(), any());
    }

    @Test
    void rejectsProcessListWithoutExactlyOnePrincipal() {
        UUID processId = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.execute(
            request(List.of(new ConvictedProcessRequest(processId, false))),
            auth
        ))
            .isInstanceOf(ValidationException.class);

        verify(processRepository, never()).findAllByUuidInAndDistrict(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsInactiveProcess() {
        UUID processId = UUID.randomUUID();
        JudicialProcess process = new JudicialProcess(
            "0003456-78.2025.8.26.0001",
            ProcessStatus.INACTIVE,
            district
        );
        process.setUuid(processId);
        when(processRepository.findAllByUuidInAndDistrict(any(), any())).thenReturn(List.of(process));

        assertThatThrownBy(() -> useCase.execute(
            request(List.of(new ConvictedProcessRequest(processId, true))),
            auth
        ))
            .isInstanceOf(ValidationException.class);

        verify(repository, never()).save(any());
    }

    private CreateConvictedRequest request(List<ConvictedProcessRequest> processes) {
        return requestWithCpf("52998224725", "Apenado de Teste", processes);
    }

    private CreateConvictedRequest requestWithCpf(
        String cpf,
        String name,
        List<ConvictedProcessRequest> processes
    ) {
        return new CreateConvictedRequest(
            name,
            cpf,
            LocalDate.of(1990, 1, 1),
            "(11) 91234-5678",
            new br.com.sicape.api.application.common.dto.request.AddressRequest(
                "12345-678", "Rua Teste", "10", null, "Centro", "Cidade", "SP"
            ),
            null,
            processes
        );
    }

    private String cpfForIndex(int index) {
        String base = String.format("%09d", 100000000 + index);
        int firstDigit = calculateCpfDigit(base, 10);
        int secondDigit = calculateCpfDigit(base + firstDigit, 11);
        return base + firstDigit + secondDigit;
    }

    private int calculateCpfDigit(String value, int startingWeight) {
        int weightedSum = 0;
        for (int index = 0; index < value.length(); index++) {
            weightedSum += Character.digit(value.charAt(index), 10) * (startingWeight - index);
        }
        int remainder = weightedSum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
