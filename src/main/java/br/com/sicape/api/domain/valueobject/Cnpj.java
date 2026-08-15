package br.com.sicape.api.domain.valueobject;

import jakarta.persistence.Embeddable;

@Embeddable
public final class Cnpj implements TaxId {
    private String value;

    /**
     * Torna o construtor vazio privado
     */
    private Cnpj() {}

    public String value() {
        return value;
    }

    public Cnpj(String value) {
        
        value = value.trim().replaceAll("\\D", "");

        if (value.isBlank() || !validate(value)) {
            throw new IllegalArgumentException("O CNPJ informado não é válido.");
        }

        this.value = value;
    }

    public static Cnpj of(String value) {
        return new Cnpj(value);
    }

    private boolean validate(String value)
    {
        if (value.length() != 14) {
            return false;
        }

        return true;
    }
}
