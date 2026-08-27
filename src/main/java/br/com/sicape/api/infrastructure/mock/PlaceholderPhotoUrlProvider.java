package br.com.sicape.api.infrastructure.mock;

import java.util.UUID;

import org.springframework.stereotype.Component;

import br.com.sicape.api.domain.provider.PhotoUrlProvider;

@Component
public class PlaceholderPhotoUrlProvider implements PhotoUrlProvider {
    private static final String PLACEHOLDER_URL = "https://placehold.co/600x600?text=Photo";

    @Override
    public String provide(UUID convictedUuid) {
        return PLACEHOLDER_URL;
    }
}
