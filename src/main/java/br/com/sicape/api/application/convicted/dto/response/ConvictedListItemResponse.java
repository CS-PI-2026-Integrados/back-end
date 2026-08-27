package br.com.sicape.api.application.convicted.dto.response;

import java.util.UUID;

public record ConvictedListItemResponse(
    UUID id,
    String name,
    String cpf,
    String photoUrl,
    String mainProcessNumber,
    long sameProcessConvictedCount
) {}
