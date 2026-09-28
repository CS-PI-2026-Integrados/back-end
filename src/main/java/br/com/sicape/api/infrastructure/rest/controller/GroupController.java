package br.com.sicape.api.infrastructure.rest.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sicape.api.application.common.dto.response.PageResponse;
import br.com.sicape.api.application.group.dto.request.CreateGroupRequest;
import br.com.sicape.api.application.group.dto.request.UpdateGroupRequest;
import br.com.sicape.api.application.group.dto.response.GroupListItemResponse;
import br.com.sicape.api.application.group.dto.response.GroupResponse;
import br.com.sicape.api.application.group.usecase.CreateGroupUseCase;
import br.com.sicape.api.application.group.usecase.DeleteGroupUseCase;
import br.com.sicape.api.application.group.usecase.GetGroupUseCase;
import br.com.sicape.api.application.group.usecase.ListGroupUseCase;
import br.com.sicape.api.application.group.usecase.AddConvictedToGroupUseCase;
import br.com.sicape.api.application.group.usecase.RemoveConvictedFromGroupUseCase;
import br.com.sicape.api.application.group.usecase.UpdateGroupUseCase;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.enums.GroupStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Validated
@RestController 
@RequiredArgsConstructor
@RequestMapping("/group")
public class GroupController {
    private final CreateGroupUseCase createUseCase;
    private final ListGroupUseCase listUseCase;
    private final GetGroupUseCase getUseCase;
    private final AddConvictedToGroupUseCase addConvictedUseCase;
    private final RemoveConvictedFromGroupUseCase removeConvictedUseCase;
    private final UpdateGroupUseCase updateUseCase;
    private final DeleteGroupUseCase deleteUseCase;

    @GetMapping
    public PageResponse<GroupListItemResponse> list(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String subject,
        @RequestParam(required = false) GroupStatus status,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @AuthenticationPrincipal AuthContext auth
    ) {
        return listUseCase.execute(name, subject, status, page, size, auth);
    }

    @GetMapping("/{uuid}")
    public GroupResponse get(
        @PathVariable UUID uuid,
        @AuthenticationPrincipal AuthContext auth
    ) {
        return getUseCase.execute(uuid, auth);
    }

    @PutMapping("/{groupId}")
    public GroupResponse update(
        @PathVariable UUID groupId,
        @Valid @RequestBody UpdateGroupRequest request,
        @AuthenticationPrincipal AuthContext auth
    ) {
        return updateUseCase.execute(groupId, request, auth);
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> delete(
        @PathVariable UUID groupId,
        @AuthenticationPrincipal AuthContext auth
    ) {
        deleteUseCase.execute(groupId, auth);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/convicted/{convictedId}")
    public ResponseEntity<GroupResponse> addConvicted(
        @PathVariable UUID groupId,
        @PathVariable UUID convictedId,
        @AuthenticationPrincipal AuthContext auth
    ) {
        addConvictedUseCase.execute(groupId, convictedId, auth);

        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/{groupId}/convicted/{convictedId}")
    public ResponseEntity<Void> removeConvicted(
        @PathVariable UUID groupId,
        @PathVariable UUID convictedId,
        @AuthenticationPrincipal AuthContext auth
    ) {
        removeConvictedUseCase.execute(groupId, convictedId, auth);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping
    public ResponseEntity<GroupResponse> create(
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
