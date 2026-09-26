package br.com.sicape.api.application.convicted.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.application.common.media.MediaAssetStore;
import br.com.sicape.api.application.common.media.MediaContent;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetConvictedPhotoUseCase {
    private final ConvictedFinder finder;
    private final MediaAssetStore media;

    @Transactional(readOnly = true)
    public MediaContent execute(UUID uuid, AuthContext authContext) {
        var convicted = finder.find(uuid, authContext);

        if (convicted.getPhoto() == null) {
            throw new ResourceNotFoundException("Foto do condenado não encontrada.");
        }
        return media.read(convicted.getPhoto());
    }
}
