package br.com.sicape.api.application.convicted.dto.response;

public record AddressResponse(
    String zipCode,
    String street,
    String number,
    String complement,
    String neighborhood,
    String city,
    String state
) {}
