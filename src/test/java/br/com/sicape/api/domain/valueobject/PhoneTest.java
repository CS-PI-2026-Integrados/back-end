package br.com.sicape.api.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneTest {
    @Test
    void formatsMobilePhone() {
        assertThat(Phone.of("11912345678").formatted()).isEqualTo("(11) 91234-5678");
    }

    @Test
    void formatsLandlinePhone() {
        assertThat(Phone.of("(11) 3234-5678").formatted()).isEqualTo("(11) 3234-5678");
    }

    @ParameterizedTest
    @ValueSource(strings = {"11912345678", "(11) 91234-5678", "11 91234-5678"})
    void normalizesMobilePhoneWithAreaCode(String value) {
        assertThat(Phone.of(value).value()).isEqualTo("11912345678");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1132345678", "(11) 3234-5678", "11 3234-5678"})
    void normalizesLandlinePhoneWithAreaCode(String value) {
        assertThat(Phone.of(value).value()).isEqualTo("1132345678");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        " ", "91234-5678", "011912345678", "119123456789", "(11) telefone", "+55 11 91234-5678",
        "11)(912345678"
    })
    void rejectsPhoneWithoutValidAreaCodeAndNumber(String value) {
        assertThatIllegalArgumentException().isThrownBy(() -> Phone.of(value));
    }
}
