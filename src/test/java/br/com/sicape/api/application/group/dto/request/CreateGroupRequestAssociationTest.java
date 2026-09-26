package br.com.sicape.api.application.group.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.sicape.api.domain.enums.GroupFrequency;

class CreateGroupRequestAssociationTest {

    @Test
    void shouldAcceptConvictedUuids() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        CreateGroupRequest request = new CreateGroupRequest(
            "Grupo reflexivo",
            "Descrição do grupo",
            "Responsabilizacao",
            List.of("Ana Beatriz", "Carlos Eduardo"),
            8,
            6,
            "14:30",
            GroupFrequency.WEEKLY,
            LocalDate.of(2026, 9, 18),
            LocalDate.of(2026, 12, 18),
            List.of(first, second)
        );

        assertThat(request.convictedUuids()).containsExactly(first, second);
    }
}
