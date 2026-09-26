package br.com.sicape.api.domain.repository;

import java.util.UUID;

public record ProcessConvictedName(UUID processUuid, String convictedName) {}