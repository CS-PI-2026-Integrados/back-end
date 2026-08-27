package br.com.sicape.api.application.convicted.dto.request;

import br.com.sicape.api.domain.valueobject.Address;
import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
    @NotBlank(message = "O CEP é obrigatório") String zipCode,
    @NotBlank(message = "O logradouro é obrigatório") String street,
    @NotBlank(message = "O número é obrigatório") String number,
    String complement,
    @NotBlank(message = "O bairro é obrigatório") String neighborhood,
    @NotBlank(message = "A cidade é obrigatória") String city,
    @NotBlank(message = "A UF é obrigatória") String state
) {
    public Address toValueObject() {
        return new Address(zipCode, street, number, complement, neighborhood, city, state);
    }
}
