package br.com.sicape.api.domain.valueobject;

import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable
public final class Cpf implements TaxId {
    private String value;

    /**
     * Torna o construtor vazio privado
     */
    private Cpf() {}

    public String value() {
        return value;
    }

    public Cpf(String value) {
        if (value == null) {
            throw new IllegalArgumentException("O CPF informado não é válido");
        }

        value = value.trim().replaceAll("\\D", "");

        if (value.isBlank() || !validate(value)) {
            throw new IllegalArgumentException("O CPF informado não é válido");
        }

        this.value = value;
    }

    public static Cpf of(String value) {
        return new Cpf(value);
    }

    private static boolean validate(String value) {
        if (value.length() != 11 || value.chars().distinct().count() == 1) {
            return false;
        }

        int firstDigit = calculateDigit(value, 9, 10);
        int secondDigit = calculateDigit(value, 10, 11);

        return firstDigit == Character.getNumericValue(value.charAt(9))
            && secondDigit == Character.getNumericValue(value.charAt(10));
    }

    private static int calculateDigit(String value, int length, int initialWeight) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += Character.getNumericValue(value.charAt(index)) * (initialWeight - index);
        }

        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    public String masked() {
        return "***." + value.substring(3, 6) + "." + value.substring(6, 9) + "-**";
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Cpf other && Objects.equals(value, other.value);
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
