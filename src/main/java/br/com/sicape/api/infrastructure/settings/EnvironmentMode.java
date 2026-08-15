package br.com.sicape.api.infrastructure.settings;

public enum EnvironmentMode {
    DEVELOPMENT,
    STAGING,
    PRODUCTION;

    public boolean isDevelopment()
    {
        return this == DEVELOPMENT;
    }

    public boolean isStaging()
    {
        return this == STAGING;
    }

    public boolean isProduction()
    {
        return this == PRODUCTION;
    }
}
