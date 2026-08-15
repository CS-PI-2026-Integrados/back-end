package br.com.sicape.api.infrastructure.settings;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class Settings {

    private final Environment environment;
    private final EnvironmentMode mode;

    public Settings(Environment environment) {
        this.environment = environment;
        this.mode = getModeFromEnvironment();
    }

    public EnvironmentMode getMode()
    {
        return this.mode;
    }

    private EnvironmentMode getModeFromEnvironment() {

        if (environment.matchesProfiles("development")) {
            return EnvironmentMode.DEVELOPMENT;
        }

        if (environment.matchesProfiles("staging")) {
            return EnvironmentMode.STAGING;
        }

        if (environment.matchesProfiles("production")) {
            return EnvironmentMode.PRODUCTION;
        }

        throw new RuntimeException("Falha ao definir o modo do projeto.");
    }
}