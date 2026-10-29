package br.com.sicape.api.application.convicted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.convicted.usecase.GetConvictedCpfUseCase;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.valueobject.Cpf;

class GetConvictedCpfUseCaseTest {
    private final ConvictedFinder finder = mock(ConvictedFinder.class);
    private final GetConvictedCpfUseCase useCase = new GetConvictedCpfUseCase(finder);
    private final UUID id = UUID.randomUUID();

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ADMIN", "OPERATOR"})
    void returnsFullCpfUsingTheAuthenticatedContext(UserRole role) {
        var auth = context(role);
        var convicted = mock(Convicted.class);
        when(convicted.getCpf()).thenReturn(Cpf.of("52998224725"));
        when(finder.find(id, auth)).thenReturn(convicted);

        assertThat(useCase.execute(id, auth).cpf()).isEqualTo("52998224725");
        verify(finder).find(id, auth);
    }

    private static AuthContext context(UserRole role) {
        var user = new User();
        user.setRole(role);
        return new AuthContext(user, new JudicialDistrict(), null);
    }
}
