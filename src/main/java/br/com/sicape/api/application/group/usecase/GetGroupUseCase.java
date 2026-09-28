package br.com.sicape.api.application.group.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.group.dto.response.GroupResponse;
import br.com.sicape.api.application.group.service.GroupFinder;
import br.com.sicape.api.application.oauth.AuthContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetGroupUseCase {
    private final GroupFinder finder;

    @Transactional(readOnly = true)
    public GroupResponse execute(UUID uuid, AuthContext authContext) {
        return GroupResponse.from(finder.find(uuid, authContext));
    }
}