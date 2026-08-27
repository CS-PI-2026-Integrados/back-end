package br.com.sicape.api.application.convicted.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RemoveConvictedUseCase {
    private final ConvictedFinder finder;
    private final ConvictedRepository repository;

    @Transactional
    public void execute(UUID uuid, AuthContext authContext) {
        Convicted convicted = finder.find(uuid, authContext);
        convicted.remove(authContext.user());
        repository.save(convicted);
    }
}
