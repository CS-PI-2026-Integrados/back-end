package br.com.sicape.api.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {
    ADMIN("admin"),
    OPERATOR("operator");

    private final String key;

    UserRole(String key) {
        this.key = key;
    }

    @JsonValue
    public String getKey() {
        return key;
    }

    @JsonCreator
    public static UserRole fromString(String value) {
        if (value == null) {
            return null;
        }
        for (UserRole role : values()) {
            if (role.name().equalsIgnoreCase(value) || role.key.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Perfil de acesso inválido: " + value);
    }
}
