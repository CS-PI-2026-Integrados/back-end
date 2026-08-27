package br.com.sicape.api.application.convicted.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.convicted.dto.response.ConvictedResponse;
import br.com.sicape.api.application.convicted.mapper.ConvictedResponseMapper;
import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.convicted.validation.PhotoValidator;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.provider.PhotoUrlProvider;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateConvictedPhotoUseCase {
    private final ConvictedFinder finder;
    private final ConvictedRepository repository;
    private final ConvictedResponseMapper mapper;
    private final PhotoValidator photoValidator;
    private final PhotoUrlProvider photoUrlProvider;

    @Transactional
    public ConvictedResponse execute(
        UUID uuid,
        byte[] content,
        String declaredContentType,
        AuthContext authContext
    ) {
        Convicted convicted = finder.find(uuid, authContext);
        photoValidator.validate(content, declaredContentType);
        convicted.completePhoto(photoUrlProvider.provide(uuid));
        return mapper.toResponse(repository.save(convicted));
    }
}
