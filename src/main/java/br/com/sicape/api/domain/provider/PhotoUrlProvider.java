package br.com.sicape.api.domain.provider;

import java.util.UUID;

public interface PhotoUrlProvider {
    String provide(UUID convictedUuid);
}
