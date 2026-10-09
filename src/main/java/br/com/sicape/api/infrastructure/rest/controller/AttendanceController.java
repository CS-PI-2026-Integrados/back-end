package br.com.sicape.api.infrastructure.rest.controller;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import br.com.sicape.api.application.attendance.dto.AttendanceResponse;
import br.com.sicape.api.application.attendance.dto.AttendanceMetricsResponse;
import br.com.sicape.api.application.attendance.usecase.GetAttendanceMetricsUseCase;
import br.com.sicape.api.application.attendance.dto.AttendanceYearsResponse;
import br.com.sicape.api.application.attendance.dto.AttendanceMonthsResponse;
import br.com.sicape.api.application.attendance.dto.CreateAttendanceRequest;
import br.com.sicape.api.application.attendance.usecase.CreateAttendanceUseCase;
import br.com.sicape.api.application.attendance.usecase.GetAttendancePhotoUseCase;
import br.com.sicape.api.application.attendance.usecase.GetAttendanceReceiptUseCase;
import br.com.sicape.api.application.attendance.usecase.GetAttendanceUseCase;
import br.com.sicape.api.application.attendance.usecase.ListAttendanceUseCase;
import br.com.sicape.api.application.attendance.usecase.ListAttendanceYearsUseCase;
import br.com.sicape.api.application.attendance.usecase.ListAttendanceMonthsUseCase;
import br.com.sicape.api.application.common.dto.response.PageResponse;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.exception.ValidationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/attendance")
public class AttendanceController {
    private final CreateAttendanceUseCase createUseCase;
    private final GetAttendancePhotoUseCase getPhoto;
    private final GetAttendanceReceiptUseCase getReceipt;
    private final ListAttendanceUseCase listUseCase;
    private final ListAttendanceYearsUseCase listYearsUseCase;
    private final ListAttendanceMonthsUseCase listMonthsUseCase;
    private final GetAttendanceUseCase getUseCase;
    private final GetAttendanceMetricsUseCase metricsUseCase;

    @GetMapping("/metrics")
    public AttendanceMetricsResponse metrics(@AuthenticationPrincipal AuthContext auth) {
        return metricsUseCase.execute(auth);
    }

    @GetMapping
    public PageResponse<AttendanceResponse> list(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) @Min(1) @Max(9998) Integer year,
        @RequestParam(required = false) @Min(1) @Max(12) Integer month,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @AuthenticationPrincipal AuthContext authContext
    ) {
        return listUseCase.execute(search, year, month, page, size, authContext);
    }

    @GetMapping("/years")
    public AttendanceYearsResponse years(@AuthenticationPrincipal AuthContext auth) {
        return listYearsUseCase.execute(auth);
    }

    @GetMapping("/months")
    public AttendanceMonthsResponse months(
        @RequestParam @Min(1) @Max(9998) int year,
        @AuthenticationPrincipal AuthContext auth
    ) {
        return listMonthsUseCase.execute(year, auth);
    }

    @GetMapping("/{uuid}")
    public AttendanceResponse get(
        @PathVariable UUID uuid,
        @AuthenticationPrincipal AuthContext auth
    ) {
        return getUseCase.execute(uuid, auth);
    }

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
        @RequestParam(defaultValue = "inline") String disposition,
        @AuthenticationPrincipal AuthContext auth
    ) {
        var contentDisposition = switch (disposition) {
            case "inline" -> ContentDisposition.inline();
            case "attachment" -> ContentDisposition.attachment();
            default -> throw new ValidationException(
                "disposition",
                "Use inline ou attachment"
            );
        };

        var receipt = getReceipt.execute(uuid, auth);

        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .headers(headers -> headers.setContentDisposition(
                contentDisposition
                .filename("comprovante-" + uuid + ".pdf")
                .build()
            ))
            .contentType(MediaType.parseMediaType(receipt.contentType()))
            .body(receipt.bytes());
    }
}
