package br.com.sicape.api.domain.valueobject;

public sealed interface TaxId permits Cpf, Cnpj {
    String value();
}
