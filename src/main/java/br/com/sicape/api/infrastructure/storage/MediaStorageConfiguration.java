package br.com.sicape.api.infrastructure.storage;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MediaStorageConfiguration {
    private final Path path;

    public MediaStorageConfiguration(
        @Value("${media.storage.path:./storage}") String path,
        @Value("${media.storage.provider:local}") String provider
    ) {
        this.path = Path.of(path).toAbsolutePath().normalize();
        if (!"local".equals(provider)) {
            throw new IllegalArgumentException("Provider de mídia não implementado; use local");
        }
    }

    @Bean
    @ConditionalOnProperty(name = "media.storage.provider", havingValue = "local", matchIfMissing = true)
    public LocalFileStorageImpl fileStorage() {
        return new LocalFileStorageImpl(path);
    }
}
