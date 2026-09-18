package br.com.sicape.api.application.common.dto.response;

import br.com.sicape.api.domain.valueobject.Address;

public record AddressResponse(
    String zipCode,
    String street,
    String number,
    String complement,
    String neighborhood,
    String city,
    String state
) {
    public static AddressResponse from(Address address) {
        return new AddressResponse(
            address.getZipCode(),
            address.getStreet(),
            address.getNumber(),
            address.getComplement(),
            address.getNeighborhood(),
            address.getCity(),
            address.getState()
        );
    }
}
