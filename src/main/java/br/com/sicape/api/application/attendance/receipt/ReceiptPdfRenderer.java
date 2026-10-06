package br.com.sicape.api.application.attendance.receipt;

import br.com.sicape.api.application.common.media.MediaContent;

public interface ReceiptPdfRenderer {
    String template();
    String logoSha256();

    // Futuramente deve receber a logo na chamada e não obte-la dentro do método
    byte[] render(ReceiptSnapshot snapshot, MediaContent convictedPhoto, MediaContent attendancePhoto);
}
