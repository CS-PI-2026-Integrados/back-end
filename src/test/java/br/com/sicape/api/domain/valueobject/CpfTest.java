package br.com.sicape.api.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CpfTest {
    @Test
    void formatsNormalizedCpf() {
        assertThat(Cpf.of("529.982.247-25").formatted()).isEqualTo("529.982.247-25");
    }
}
