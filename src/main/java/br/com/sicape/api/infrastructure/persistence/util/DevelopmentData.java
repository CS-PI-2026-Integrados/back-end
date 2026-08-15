package br.com.sicape.api.infrastructure.persistence.util;

import java.util.UUID;

public class DevelopmentData {
    public static UUID mockUuid(int value)
    {
        return new UUID(0L, value);
    } 
}
