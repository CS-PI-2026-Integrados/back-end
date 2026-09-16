package br.com.sicape.api.infrastructure.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sicape.api.application.group.dto.request.CreateGroupRequest;
import br.com.sicape.api.application.group.usecase.CreateGroupUseCase;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Group;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequiredArgsConstructor
@RequestMapping("/group")
public class GroupController {
    private final CreateGroupUseCase createUseCase;
    
    public ResponseEntity<Group> create(
        @Valid
        @RequestBody
        CreateGroupRequest request,

        @AuthenticationPrincipal 
        AuthContext auth
    ) {
        return ResponseEntity.status(201).body(
            createUseCase.execute(request, auth)
        );
    }
}
