package br.com.sicape.api.application.attendance.usecase;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.attendance.receipt.ReceiptPdfRenderer;
import br.com.sicape.api.application.attendance.receipt.ReceiptSnapshot;
import br.com.sicape.api.application.common.media.MediaAssetStore;
import br.com.sicape.api.application.common.media.MediaContent;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.MediaAsset;
import br.com.sicape.api.domain.enums.MediaAssetKind;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetAttendanceReceiptUseCase {
    private final AttendanceRepository attendances;
    private final MediaAssetStore media;
    private final ReceiptPdfRenderer renderer;
    private final ObjectMapper objectMapper;

    @Transactional
    public MediaContent execute(UUID uuid, AuthContext auth) {
        var attendance = attendances.findForReceipt(uuid, auth.district())
            .orElseThrow(() -> new ResourceNotFoundException("Presença não encontrada."));

        if (attendance.getReceipt() != null) {
            return readPersisted(uuid, attendance.getReceipt());
        }

        if (attendance.getPhoto() == null) {
            log.error("Presença sem referência de foto; attendanceUuid={}", uuid);
            throw new IllegalStateException("A foto vinculada à presença está indisponível.");
        }
        var convictedPhotoAsset = attendance.getConvicted().getPhoto();
        if (convictedPhotoAsset == null) {
            throw new ConflictException("A foto de cadastro do apenado é obrigatória para emitir o comprovante.");
        }

        MediaContent attendancePhoto;
        try {
            attendancePhoto = media.read(attendance.getPhoto());
        } catch (RuntimeException failure) {
            log.error("Falha de integridade na foto da presença; attendanceUuid={}, photoAssetUuid={}",
                uuid, attendance.getPhoto().getUuid(), failure);
            throw new IllegalStateException("A foto vinculada à presença está indisponível.", failure);
        }
        MediaContent convictedPhoto;
        try {
            convictedPhoto = media.read(convictedPhotoAsset);
        } catch (RuntimeException failure) {
            log.error("Falha de integridade na foto do apenado; attendanceUuid={}, photoAssetUuid={}",
                uuid, convictedPhotoAsset.getUuid(), failure);
            throw new IllegalStateException("A foto de cadastro do apenado está indisponível.", failure);
        }

        var snapshot = ReceiptSnapshot.create(attendance, renderer.template(), renderer.logoSha256());
        byte[] pdf = renderer.render(snapshot, convictedPhoto, attendancePhoto);
        var receipt = media.save(pdf, "application/pdf", MediaAssetKind.RECEIPT);
        attendance.completeReceipt(receipt);
        attendances.saveAndFlush(attendance);
        return readPersisted(uuid, receipt);
    }

    private MediaContent readPersisted(UUID attendanceUuid, MediaAsset asset) {
        try {
            return media.read(asset);
        } catch (RuntimeException failure) {
            log.error("Comprovante emitido indisponível; attendanceUuid={}, receiptAssetUuid={}",
                attendanceUuid, asset.getUuid(), failure);
            throw new IllegalStateException("O comprovante emitido está indisponível.", failure);
        }
    }

}
