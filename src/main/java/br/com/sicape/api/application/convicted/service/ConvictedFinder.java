package br.com.sicape.api.application.convicted.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.enums.ConvictedStatus;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConvictedFinder {
    private final ConvictedRepository repository;

    public Convicted find(UUID uuid, AuthContext authContext) {
        return repository.findByUuidAndDistrictAndStatusNot(
            uuid,
            authContext.district(),
            ConvictedStatus.INACTIVE
        ).orElseThrow(() -> new ResourceNotFoundException("Condenado não encontrado."));
    }
}
