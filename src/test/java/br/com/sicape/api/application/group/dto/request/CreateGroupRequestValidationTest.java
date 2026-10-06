package br.com.sicape.api.application.group.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.sicape.api.domain.enums.GroupFrequency;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CreateGroupRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldRejectInvalidMeetingTimeAndRelationships() {

        List<UUID> convictedUuids = Arrays.asList();

        CreateGroupRequest request = new CreateGroupRequest(
            "",
            "",
            "",
            List.of(),
            4,
            5,
            "25:99",
            GroupFrequency.WEEKLY,
            LocalDate.of(2026, 9, 20),
            LocalDate.of(2026, 9, 15),
            convictedUuids
        );

        Set<ConstraintViolation<CreateGroupRequest>> violations = validator.validate(request);

        assertThat(violations)
            .extracting(ConstraintViolation::getMessage)
            .contains(
                "O nome do grupo é obrigatório",
                "Informe pelo menos um ministrante para o grupo",
                "A descrição do grupo é obrigatória",
                "A quantidade mínima de encontros não pode ser maior que o total de encontros",
                "A hora da reunião deve estar no formato H:i",
                "A data prevista de término não pode ser anterior à data de início"
            );
    }
}
