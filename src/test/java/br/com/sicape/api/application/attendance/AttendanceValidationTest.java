package br.com.sicape.api.application.attendance;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import br.com.sicape.api.application.common.validation.PhotoValidator;
import br.com.sicape.api.domain.exception.PayloadTooLargeException;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.valueobject.Address;

class AttendanceValidationTest {
    private final PhotoValidator photos = new PhotoValidator();

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "12345-678", "abc12345678", "12.345-678"})
    void normalizesZipCode(String zip) {
        var address = address(zip);
        assertThat(address.getZipCode()).isEqualTo("12345678");
        assertThat(address.getComplement()).isNull();
        assertThat(address.getState()).isEqualTo("SP");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1234", "123456789", "     "})
    void rejectsInvalidZipCodes(String zip) {
        assertThatIllegalArgumentException().isThrownBy(() -> address(zip));
    }

    @Test
    void requiresEveryAddressFieldExceptComplement() {
        String[] fields = {"12345678", "Rua", "1", null, "Bairro", "Cidade", "SP"};
        for (int index : new int[]{0, 1, 2, 4, 5, 6}) {
            var invalid = fields.clone();
            invalid[index] = " ";
            assertThatIllegalArgumentException().isThrownBy(() -> new Address(
                invalid[0], invalid[1], invalid[2], invalid[3], invalid[4], invalid[5], invalid[6]));
        }
    }

    @Test
    void validatesSignaturesAndDetectsContentTypes() {
        byte[] png = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
        assertThat(photos.validate(png, "image/png")).isEqualTo("image/png");
        assertThat(photos.validate(jpeg(3), "image/jpeg")).isEqualTo("image/jpeg");
        assertThatThrownBy(() -> photos.validate(png, "image/jpeg")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> photos.validate(new byte[]{1, 2, 3}, "image/png")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> photos.validate(png, null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> photos.validate(jpeg(3), "image/jpg")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> photos.validate(new byte[0], "image/png")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> photos.validate(null, "image/png")).isInstanceOf(ValidationException.class);
    }

    @Test
    void enforcesFiveMebibytes() {
        assertThatCode(() -> photos.validate(jpeg(5 * 1024 * 1024), "image/jpeg")).doesNotThrowAnyException();
        assertThatThrownBy(() -> photos.validate(jpeg(5 * 1024 * 1024 + 1), "image/jpeg"))
            .isInstanceOf(PayloadTooLargeException.class);
    }

    public static byte[] jpeg(int size) {
        byte[] content = new byte[size];
        content[0] = (byte)0xff;
        content[1] = (byte)0xd8;
        content[2] = (byte)0xff;
        return content;
    }

    private Address address(String zip) {
        return new Address(zip, "Rua", "1", " ", "Bairro", "Cidade", "sp");
    }
}
