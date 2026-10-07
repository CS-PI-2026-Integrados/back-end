package br.com.sicape.api.application.convicted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.convicted.usecase.UpdateConvictedStatusUseCase;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.enums.ConvictedStatus;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.valueobject.Address;
import br.com.sicape.api.domain.valueobject.Cpf;
import br.com.sicape.api.domain.valueobject.Phone;

class UpdateConvictedStatusUseCaseTest {
    @Test
    void doesNotSaveWhenRequestedStatusAlreadyMatches() {
        var finder = mock(ConvictedFinder.class);
        var repository = mock(ConvictedRepository.class);
        var auth = new AuthContext(null, new JudicialDistrict(), null);
        var convicted = new Convicted("Apenado", Cpf.of("52998224725"), LocalDate.of(1990, 1, 1),
            Phone.of("11912345678"), new Address("12345678", "Rua", "10", null, "Centro", "Cidade", "SP"),
            null, auth.district());
        UUID id = UUID.randomUUID();
        when(finder.findForUpdate(id, auth)).thenReturn(convicted);
        var useCase = new UpdateConvictedStatusUseCase(finder, repository);

        for (var status : ConvictedStatus.values()) {
            convicted.updateStatus(status, null);
            var timestamp = convicted.getDeactivatedAt();
            assertThat(useCase.execute(id, status, auth).status()).isEqualTo(status);
            assertThat(convicted.getDeactivatedAt()).isEqualTo(timestamp);
        }
        verifyNoInteractions(repository);
    }
}
