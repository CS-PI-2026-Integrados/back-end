package br.com.sicape.api.domain.enums;

public enum EmploymentStatus {
    FORMAL_WORK,
    INFORMAL_WORK,
    UNEMPLOYED;

    public String displayName() {
        return switch (this) {
            case FORMAL_WORK -> "Trabalho formal";
            case INFORMAL_WORK -> "Trabalho informal";
            case UNEMPLOYED -> "Desempregado";
        };
    }
}
