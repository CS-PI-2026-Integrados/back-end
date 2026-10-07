package br.com.sicape.api.application.convicted.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.convicted.dto.response.ConvictedResponse;
import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.enums.ConvictedStatus;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateConvictedStatusUseCase {
    private final ConvictedFinder finder;
    private final ConvictedRepository repository;

    @Transactional
    public ConvictedResponse execute(UUID uuid, ConvictedStatus status, AuthContext authContext) {
        var convicted = finder.findForUpdate(uuid, authContext);
        if (convicted.getStatus() == status) {
            return ConvictedResponse.from(convicted);
        }
        convicted.updateStatus(status, authContext.user());
        return ConvictedResponse.from(repository.save(convicted));
    }
}
