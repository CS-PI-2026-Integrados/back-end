package br.com.sicape.api.domain.repository;

import java.util.UUID;

public record ProcessConvictedCount(UUID processUuid, long count) {}
