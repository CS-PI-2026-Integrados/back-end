package br.com.sicape.api.domain.valueobject;

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
        
        value = value.trim().replaceAll("\\D", "");

        if (value.isBlank() || !validate(value)) {
            throw new IllegalArgumentException("Cpf inválido: " + value);
        }

        this.value = value;
    }

    public static Cpf of(String value) {
        return new Cpf(value);
    }

    private boolean validate(String value)
    {
        if (value.length() != 11) {
            return false;
        }

        return true;
    }
}
