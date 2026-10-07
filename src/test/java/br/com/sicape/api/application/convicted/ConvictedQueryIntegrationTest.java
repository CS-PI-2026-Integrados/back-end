package br.com.sicape.api.application.convicted;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import br.com.sicape.api.application.convicted.dto.response.ConvictedListItemResponse;
import br.com.sicape.api.application.convicted.usecase.ListConvictedUseCase;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.ConvictedProcess;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.JudicialProcess;
import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.enums.ConvictedStatus;
import br.com.sicape.api.domain.enums.EmploymentStatus;
import br.com.sicape.api.domain.enums.ProcessStatus;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.valueobject.Address;
import br.com.sicape.api.domain.valueobject.Cpf;
import br.com.sicape.api.domain.valueobject.Phone;

@DataJpaTest(showSql = false, properties = {
    "spring.datasource.url=jdbc:h2:mem:convicted-query;MODE=MySQL;NON_KEYWORDS=USER,VALUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ListConvictedUseCase.class)
class ConvictedQueryIntegrationTest {
    @Autowired private TestEntityManager entityManager;
    @Autowired private ListConvictedUseCase listUseCase;

    private AuthContext auth;
    private Convicted active;
    private Convicted inactive;
    private Convicted otherActive;

    @BeforeEach
    void setUp() {
        auth = createAuth("Comarca Central", "52998224725");
        var foreignAuth = createAuth("Outra Comarca", "12345678909");
        var process = entityManager.persistAndFlush(new JudicialProcess(
            "0001234-56.2026.8.26.0001", ProcessStatus.INACTIVE, auth.district()));
        var secondProcess = entityManager.persistAndFlush(new JudicialProcess(
            "0009876-54.2026.8.26.0001", ProcessStatus.INACTIVE, auth.district()));

        active = createConvicted("Arthur Silva", "11144477735", auth, ConvictedStatus.ACTIVE);
        inactive = createConvicted("Maria Silva", "39053344705", auth, ConvictedStatus.INACTIVE);
        otherActive = createConvicted("Zelia Silva", "16899535009", auth, ConvictedStatus.ACTIVE);
        for (var convicted : List.of(active, inactive, otherActive)) {
            convicted.replaceProcesses(List.of(new ConvictedProcess(convicted, process, true),
                new ConvictedProcess(convicted, secondProcess, false)));
        }
        createConvicted("Arthur Silva", "12345678909", foreignAuth, ConvictedStatus.ACTIVE);
        createConvicted("Maria Silva", "52998224725", foreignAuth, ConvictedStatus.INACTIVE);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void listsAllStatusesWithinDistrictWithDistinctResultsAndPagination() {
        var first = listUseCase.execute(null, null, 0, 2, auth);
        var second = listUseCase.execute(null, null, 1, 2, auth);

        assertThat(first.content()).extracting(ConvictedListItemResponse::id)
            .containsExactly(active.getUuid(), inactive.getUuid());
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(second.content()).extracting(ConvictedListItemResponse::id)
            .containsExactly(otherActive.getUuid());
        assertThat(second.totalElements()).isEqualTo(3);
    }

    @ParameterizedTest
    @EnumSource(ConvictedStatus.class)
    void filtersStatusAndSearchWithinDistrictWithCorrectPagination(ConvictedStatus status) {
        long expectedCount = status == ConvictedStatus.ACTIVE ? 2 : 1;
        var expectedFirst = status == ConvictedStatus.ACTIVE ? active : inactive;
        for (String search : new String[]{null, "  SILVA  ", "0001234-56.2026.8.26.0001", "00012345620268260001"}) {
            var result = listUseCase.execute(search, status, 0, 1, auth);

            assertThat(result.content()).as("Busca por %s com %s", search, status)
                .extracting(ConvictedListItemResponse::id).containsExactly(expectedFirst.getUuid());
            assertThat(result.totalElements()).isEqualTo(expectedCount);
            assertThat(result.totalPages()).isEqualTo((int) expectedCount);
        }
        var second = listUseCase.execute("Silva", status, 1, 1, auth);
        assertThat(second.content()).extracting(ConvictedListItemResponse::id)
            .containsExactlyElementsOf(status == ConvictedStatus.ACTIVE ? List.of(otherActive.getUuid()) : List.of());
        assertThat(second.totalElements()).isEqualTo(expectedCount);
    }

    @Test
    void searchesInactiveCpfWithAndWithoutStatusFilter() {
        for (String search : new String[]{"390.533.447-05", "39053344705"}) {
            for (ConvictedStatus status : new ConvictedStatus[]{null, ConvictedStatus.INACTIVE}) {
                var result = listUseCase.execute(search, status, 0, 1, auth);
                assertThat(result.content()).extracting(ConvictedListItemResponse::id)
                    .containsExactly(inactive.getUuid());
                assertThat(result.totalElements()).isEqualTo(1);
            }
            assertThat(listUseCase.execute(search, ConvictedStatus.ACTIVE, 0, 1, auth).content()).isEmpty();
        }
    }

    @Test
    void keepsCountingOnlyActiveConvictedInSameProcessRegardlessOfListFilter() {
        for (ConvictedStatus status : new ConvictedStatus[]{null, ConvictedStatus.ACTIVE, ConvictedStatus.INACTIVE}) {
            var result = listUseCase.execute(null, status, 0, 20, auth);
            assertThat(result.content()).isNotEmpty().allSatisfy(item -> {
                assertThat(item.sameProcessConvictedCount()).isEqualTo(2);
                assertThat(item.mainProcessNumber()).isEqualTo("0001234-56.2026.8.26.0001");
            });
        }
    }

    private AuthContext createAuth(String name, String cpf) {
        var district = new JudicialDistrict();
        district.setName(name);
        entityManager.persistAndFlush(district);
        var user = entityManager.persistAndFlush(new User("Operador", Cpf.of(cpf),
            cpf + "@test.local", "hash", UserRole.OPERATOR, district));
        return new AuthContext(user, district, null);
    }

    private Convicted createConvicted(String name, String cpf, AuthContext context, ConvictedStatus status) {
        var convicted = new Convicted(name, Cpf.of(cpf), LocalDate.of(1990, 1, 1),
            Phone.of("11988881001"), new Address("12345678", "Rua Central", "10", null, "Centro", "Cidade", "SP"),
            EmploymentStatus.FORMAL_WORK, context.district());
        if (status == ConvictedStatus.INACTIVE) {
            convicted.remove(context.user());
        }
        return entityManager.persistAndFlush(convicted);
    }
}
