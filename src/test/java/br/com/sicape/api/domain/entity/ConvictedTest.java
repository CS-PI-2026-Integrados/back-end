package br.com.sicape.api.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import br.com.sicape.api.domain.enums.ConvictedStatus;
import br.com.sicape.api.domain.enums.EmploymentStatus;
import br.com.sicape.api.domain.enums.MediaAssetKind;
import br.com.sicape.api.domain.enums.ProcessStatus;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.valueobject.Address;
import br.com.sicape.api.domain.valueobject.Cpf;
import br.com.sicape.api.domain.valueobject.Phone;

class ConvictedTest {
    @Test
    void changesStatusIdempotentlyAndPreservesDataAndLinks() {
        var convicted = convicted();
        var user = new User();
        var process = new JudicialProcess("0001234-56.2026.8.26.0001", ProcessStatus.INACTIVE, convicted.getDistrict());
        var link = new ConvictedProcess(convicted, process, true);
        var photo = photo();
        convicted.replaceProcesses(List.of(link));
        convicted.completePhoto(photo);

        convicted.updateStatus(ConvictedStatus.INACTIVE, user);
        var deactivatedAt = convicted.getDeactivatedAt();
        assertThat(deactivatedAt).isNotNull();
        assertThat(convicted.getDeactivatedBy()).isSameAs(user);

        convicted.updateStatus(ConvictedStatus.INACTIVE, new User());
        assertThat(convicted.getDeactivatedAt()).isEqualTo(deactivatedAt);
        assertThat(convicted.getDeactivatedBy()).isSameAs(user);

        convicted.updateStatus(ConvictedStatus.ACTIVE, user);
        convicted.updateStatus(ConvictedStatus.ACTIVE, user);
        assertThat(convicted.getStatus()).isEqualTo(ConvictedStatus.ACTIVE);
        assertThat(convicted.getDeactivatedAt()).isNull();
        assertThat(convicted.getDeactivatedBy()).isNull();
        assertThat(convicted.getPhoto()).isSameAs(photo);
        assertThat(convicted.getProcesses()).containsExactly(link);
        assertThat(convicted.getCpf()).isEqualTo(Cpf.of("52998224725"));
        convicted.updateName("Novo nome");
        assertThat(convicted.getName()).isEqualTo("Novo nome");
    }

    @Test
    void rejectsDeactivationWithActiveProcessWithoutChangingAudit() {
        var convicted = convicted();
        var process = new JudicialProcess("0001234-56.2026.8.26.0001", ProcessStatus.ACTIVE, convicted.getDistrict());
        convicted.replaceProcesses(List.of(new ConvictedProcess(convicted, process, true)));

        assertThatThrownBy(() -> convicted.updateStatus(ConvictedStatus.INACTIVE, new User()))
            .isInstanceOf(ConflictException.class);
        assertThat(convicted.getStatus()).isEqualTo(ConvictedStatus.ACTIVE);
        assertThat(convicted.getDeactivatedAt()).isNull();
        assertThat(convicted.getDeactivatedBy()).isNull();
    }

    @ParameterizedTest
    @MethodSource("mutations")
    void rejectsAllDataMutationsWhileInactive(Consumer<Convicted> mutation) {
        var convicted = convicted();
        convicted.updateStatus(ConvictedStatus.INACTIVE, null);
        assertThatThrownBy(() -> mutation.accept(convicted))
            .isInstanceOf(ConflictException.class).hasMessageContaining("Reative");
        assertThat(convicted.getName()).isEqualTo("Apenado");
        assertThat(convicted.getProcesses()).isEmpty();
        assertThat(convicted.getPhoto()).isNull();
    }

    static Stream<Consumer<Convicted>> mutations() {
        return Stream.of(
            c -> c.updateName("Novo nome"),
            c -> c.updateCpf(Cpf.of("11144477735")),
            c -> c.updateBirthDate(LocalDate.of(1991, 1, 1)),
            c -> c.updatePhone(Phone.of("11988881001")),
            c -> c.updateAddress(new Address("12345678", "Outra rua", "20", null, "Centro", "Cidade", "SP")),
            c -> c.updateEmploymentStatus(EmploymentStatus.UNEMPLOYED),
            c -> c.replaceProcesses(List.of()),
            c -> c.completePhoto(photo())
        );
    }

    private static Convicted convicted() {
        return new Convicted("Apenado", Cpf.of("52998224725"), LocalDate.of(1990, 1, 1),
            Phone.of("11912345678"), new Address("12345678", "Rua", "10", null, "Centro", "Cidade", "SP"),
            null, new JudicialDistrict());
    }

    private static MediaAsset photo() {
        UUID id = UUID.randomUUID();
        return new MediaAsset(id, id.toString(), "image/jpeg", 3, MediaAssetKind.PHOTO);
    }
}
