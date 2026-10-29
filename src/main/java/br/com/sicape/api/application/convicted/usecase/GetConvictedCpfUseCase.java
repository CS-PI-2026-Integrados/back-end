package br.com.sicape.api.application.convicted.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.convicted.dto.response.ConvictedCpfResponse;
import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.oauth.AuthContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetConvictedCpfUseCase {
    private final ConvictedFinder finder;

    @Transactional(readOnly = true)
    public ConvictedCpfResponse execute(UUID uuid, AuthContext authContext) {

        var convicted = finder.find(uuid, authContext);
        return new ConvictedCpfResponse(convicted.getCpf().value());
    }
}
