package br.com.sicape.api.infrastructure.rest.controller;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.sicape.api.application.convicted.dto.request.CreateConvictedRequest;
import br.com.sicape.api.application.convicted.dto.request.UpdateConvictedRequest;
import br.com.sicape.api.application.convicted.dto.response.ConvictedListItemResponse;
import br.com.sicape.api.application.convicted.dto.response.ConvictedResponse;
import br.com.sicape.api.application.common.dto.response.PageResponse;
import br.com.sicape.api.application.convicted.usecase.CreateConvictedUseCase;
import br.com.sicape.api.application.convicted.usecase.GetConvictedPhotoUseCase;
import br.com.sicape.api.application.convicted.usecase.GetConvictedUseCase;
import br.com.sicape.api.application.convicted.usecase.ListConvictedUseCase;
import br.com.sicape.api.application.convicted.usecase.RemoveConvictedUseCase;
import br.com.sicape.api.application.convicted.usecase.UpdateConvictedPhotoUseCase;
import br.com.sicape.api.application.convicted.usecase.UpdateConvictedUseCase;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.exception.ValidationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/convicted")
public class ConvictedController {
    private final CreateConvictedUseCase createUseCase;
    private final ListConvictedUseCase listUseCase;
    private final GetConvictedUseCase getUseCase;
    private final UpdateConvictedUseCase updateUseCase;
    private final RemoveConvictedUseCase removeUseCase;
    private final UpdateConvictedPhotoUseCase updatePhotoUseCase;
    private final GetConvictedPhotoUseCase getPhotoUseCase;

    @PostMapping
    public ResponseEntity<ConvictedResponse> create(
        @Valid @RequestBody CreateConvictedRequest request,
        @AuthenticationPrincipal AuthContext authContext
    ) {
        ConvictedResponse response = createUseCase.execute(request, authContext);
        return ResponseEntity.created(URI.create("/api/convicted/" + response.id())).body(response);
    }

    @GetMapping
    public PageResponse<ConvictedListItemResponse> list(
        @RequestParam(name = "search", required = false) String query,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @AuthenticationPrincipal AuthContext authContext
    ) {
        return listUseCase.execute(query, page, size, authContext);
    }

    @GetMapping("/{uuid}")
    public ConvictedResponse get(
        @PathVariable UUID uuid,
        @AuthenticationPrincipal AuthContext authContext
    ) {
        return getUseCase.execute(uuid, authContext);
    }

    @PutMapping("/{uuid}")
    public ConvictedResponse update(
        @PathVariable UUID uuid,
        @RequestBody UpdateConvictedRequest request,
        @AuthenticationPrincipal AuthContext authContext
    ) {
        return updateUseCase.execute(uuid, request, authContext);
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> remove(
        @PathVariable UUID uuid,
        @AuthenticationPrincipal AuthContext authContext
    ) {
        removeUseCase.execute(uuid, authContext);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{uuid}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ConvictedResponse updatePhoto(
        @PathVariable UUID uuid,
        @RequestPart("photo") MultipartFile photo,
        @AuthenticationPrincipal AuthContext authContext
    ) {
        try {
            return updatePhotoUseCase.execute(uuid, photo.getBytes(), photo.getContentType(), authContext);
        } catch (IOException exception) {
            throw new ValidationException("photo", "Não foi possível ler a foto enviada");
        }
    }

    @GetMapping("/{uuid}/photo")
    public ResponseEntity<Void> getPhoto(
        @PathVariable UUID uuid,
        @AuthenticationPrincipal AuthContext authContext
    ) {
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(getPhotoUseCase.execute(uuid, authContext)))
            .build();
    }
}
