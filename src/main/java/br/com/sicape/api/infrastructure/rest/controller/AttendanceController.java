package br.com.sicape.api.infrastructure.rest.controller;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import br.com.sicape.api.application.attendance.dto.AttendanceResponse;
import br.com.sicape.api.application.attendance.dto.CreateAttendanceRequest;
import br.com.sicape.api.application.attendance.usecase.CreateAttendanceUseCase;
import br.com.sicape.api.application.attendance.usecase.GetAttendancePhotoUseCase;
import br.com.sicape.api.application.attendance.usecase.GetAttendanceReceiptUseCase;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.exception.ValidationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/attendance")
public class AttendanceController {
    private final CreateAttendanceUseCase createUseCase;
    private final GetAttendancePhotoUseCase getPhoto;
    private final GetAttendanceReceiptUseCase getReceipt;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttendanceResponse> create(
        @Valid @RequestPart("data") CreateAttendanceRequest data,
        @RequestPart("photo") MultipartFile photo,
        @AuthenticationPrincipal AuthContext auth
    ) {
        byte[] content;
        try {
            content = photo.getBytes();
        } catch (IOException exception) {
            throw new ValidationException("photo", "Não foi possível ler a foto enviada");
        }
        AttendanceResponse response = createUseCase.execute(data, content, photo.getContentType(), auth);
        return ResponseEntity.created(URI.create("/api/attendance/" + response.id())).body(response);
    }

    @GetMapping("/{uuid}/photo")
    public ResponseEntity<byte[]> getPhoto(
        @PathVariable UUID uuid,
        @AuthenticationPrincipal AuthContext auth
    ) {
        var photo = getPhoto.execute(uuid, auth);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .contentType(MediaType.parseMediaType(photo.contentType()))
            .body(photo.bytes());
    }

    @GetMapping("/{uuid}/receipt")
    public ResponseEntity<byte[]> getReceipt(
        @PathVariable UUID uuid,
        @AuthenticationPrincipal AuthContext auth
    ) {
        var receipt = getReceipt.execute(uuid, auth);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .contentType(MediaType.parseMediaType(receipt.contentType()))
            .body(receipt.bytes());
    }
}
