package br.com.sicape.api.application.convicted.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.common.media.MediaAssetStore;
import br.com.sicape.api.application.common.validation.PhotoValidator;
import br.com.sicape.api.application.convicted.dto.response.ConvictedResponse;
import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.MediaAsset;
import br.com.sicape.api.domain.enums.MediaAssetKind;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateConvictedPhotoUseCase {
    private final ConvictedFinder finder;
    private final ConvictedRepository repository;
    private final PhotoValidator photoValidator;
    private final MediaAssetStore media;

    @Transactional
    public ConvictedResponse execute(
        UUID uuid,
        byte[] content,
        String declaredContentType,
        AuthContext authContext
    ) {
        String contentType = photoValidator.validate(content, declaredContentType);

        Convicted convicted = finder.find(uuid, authContext);
        MediaAsset replaced = convicted.getPhoto();
        MediaAsset created = media.save(content, contentType, MediaAssetKind.PHOTO);

        convicted.completePhoto(created);
        repository.saveAndFlush(convicted);
        if (replaced != null) {
            media.removeAfterCommit(replaced);
        }
        return ConvictedResponse.from(convicted);
    }
}
