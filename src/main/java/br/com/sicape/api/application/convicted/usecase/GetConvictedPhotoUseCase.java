package br.com.sicape.api.application.convicted.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetConvictedPhotoUseCase {
    private final ConvictedFinder finder;

    @Transactional(readOnly = true)
    public String execute(UUID uuid, AuthContext authContext) {
        Convicted convicted = finder.find(uuid, authContext);
        if (convicted.getPhotoUrl() == null) {
            throw new ResourceNotFoundException("Foto do condenado não encontrada.");
        }
        return convicted.getPhotoUrl();
    }
}
