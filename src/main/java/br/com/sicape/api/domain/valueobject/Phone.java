package br.com.sicape.api.domain.valueobject;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public final class Phone {
    private static final String INVALID_PHONE = "O telefone deve possuir DDD e número com 8 ou 9 dígitos";
    private static final String PHONE_FORMAT = "(?:[1-9]\\d|\\([1-9]\\d\\))\\s?\\d{4,5}-?\\d{4}";

    @Column(name = "phone", nullable = false, length = 11)
    private String value;

    private Phone() {}

    public Phone(String value) {
        if (value == null || !value.trim().matches(PHONE_FORMAT)) {
            throw new IllegalArgumentException(INVALID_PHONE);
        }

        String digits = value.replaceAll("\\D", "");
        this.value = digits;
    }

    public static Phone of(String value) {
        return new Phone(value);
    }

    public String value() {
        return value;
    }

    public String formatted() {
        return "(" + value.substring(0, 2) + ") " + value.substring(2, value.length() - 4)
            + "-" + value.substring(value.length() - 4);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Phone other && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
