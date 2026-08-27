package br.com.sicape.api.application.convicted.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;

import br.com.sicape.api.domain.valueobject.Address;
import lombok.Getter;

@Getter
public class UpdateAddressRequest {
    private String zipCode;
    private String street;
    private String number;
    private String complement;
    private String neighborhood;
    private String city;
    private String state;

    private boolean zipCodeProvided;
    private boolean streetProvided;
    private boolean numberProvided;
    private boolean complementProvided;
    private boolean neighborhoodProvided;
    private boolean cityProvided;
    private boolean stateProvided;

    @JsonSetter("zip_code")
    public void setZipCode(String value) { this.zipCode = value; this.zipCodeProvided = true; }

    @JsonSetter("street")
    public void setStreet(String value) { this.street = value; this.streetProvided = true; }

    @JsonSetter("number")
    public void setNumber(String value) { this.number = value; this.numberProvided = true; }

    @JsonSetter("complement")
    public void setComplement(String value) { this.complement = value; this.complementProvided = true; }

    @JsonSetter("neighborhood")
    public void setNeighborhood(String value) { this.neighborhood = value; this.neighborhoodProvided = true; }

    @JsonSetter("city")
    public void setCity(String value) { this.city = value; this.cityProvided = true; }

    @JsonSetter("state")
    public void setState(String value) { this.state = value; this.stateProvided = true; }

    public Address apply(Address current) {
        rejectNullRequired();
        return current.update(
            zipCodeProvided ? zipCode : null,
            streetProvided ? street : null,
            numberProvided ? number : null,
            complementProvided,
            complement,
            neighborhoodProvided ? neighborhood : null,
            cityProvided ? city : null,
            stateProvided ? state : null
        );
    }

    private void rejectNullRequired() {
        if (zipCodeProvided && zipCode == null
            || streetProvided && street == null
            || numberProvided && number == null
            || neighborhoodProvided && neighborhood == null
            || cityProvided && city == null
            || stateProvided && state == null) {
            throw new IllegalArgumentException("Campos obrigatórios do endereço não podem ser nulos");
        }
    }
}
