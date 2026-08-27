package br.com.sicape.api.domain.valueobject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {
    @Column(name = "zip_code", nullable = false, length = 8)
    private String zipCode;

    @Column(name = "street", nullable = false, length = 255)
    private String street;

    @Column(name = "address_number", nullable = false, length = 30)
    private String number;

    @Column(name = "address_complement", length = 255)
    private String complement;

    @Column(name = "neighborhood", nullable = false, length = 120)
    private String neighborhood;

    @Column(name = "city", nullable = false, length = 120)
    private String city;

    @Column(name = "state", nullable = false, length = 2)
    private String state;

    public Address(
        String zipCode,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state
    ) {
        this.zipCode = normalizeZipCode(zipCode);
        this.street = required(street, "logradouro");
        this.number = required(number, "número");
        this.complement = normalizeOptional(complement);
        this.neighborhood = required(neighborhood, "bairro");
        this.city = required(city, "cidade");
        this.state = normalizeState(state);
    }

    public Address update(
        String zipCode,
        String street,
        String number,
        boolean complementProvided,
        String complement,
        String neighborhood,
        String city,
        String state
    ) {
        return new Address(
            zipCode == null ? this.zipCode : zipCode,
            street == null ? this.street : street,
            number == null ? this.number : number,
            complementProvided ? complement : this.complement,
            neighborhood == null ? this.neighborhood : neighborhood,
            city == null ? this.city : city,
            state == null ? this.state : state
        );
    }

    private static String normalizeZipCode(String value) {
        String normalized = required(value, "CEP").replaceAll("\\D", "");
        if (normalized.length() != 8) {
            throw new IllegalArgumentException("O CEP deve possuir 8 dígitos");
        }
        return normalized;
    }

    private static String normalizeState(String value) {
        String normalized = required(value, "UF").toUpperCase();
        if (!normalized.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("A UF deve possuir 2 letras");
        }
        return normalized;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("O campo " + field + " é obrigatório");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
