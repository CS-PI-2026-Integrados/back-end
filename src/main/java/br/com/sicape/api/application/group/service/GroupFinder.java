package br.com.sicape.api.application.group.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Group;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.repository.GroupRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GroupFinder {
    private final GroupRepository repository;

    public Group find(UUID uuid, AuthContext authContext) {
        return repository.findByUuidAndDistrict(uuid, authContext.district())
            .orElseThrow(() -> new ResourceNotFoundException("Grupo reflexivo não encontrado."));
    }
}