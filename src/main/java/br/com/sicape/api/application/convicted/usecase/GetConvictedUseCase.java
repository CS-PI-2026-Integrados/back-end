package br.com.sicape.api.application.convicted.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.convicted.dto.response.ConvictedResponse;
import br.com.sicape.api.application.convicted.mapper.ConvictedResponseMapper;
import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.oauth.AuthContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetConvictedUseCase {
    private final ConvictedFinder finder;
    private final ConvictedResponseMapper mapper;

    @Transactional(readOnly = true)
    public ConvictedResponse execute(UUID uuid, AuthContext authContext) {
        return mapper.toResponse(finder.find(uuid, authContext));
    }
}
