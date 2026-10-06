package br.com.sicape.api.infrastructure.receipt;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import javax.imageio.ImageIO;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import br.com.sicape.api.application.attendance.receipt.ReceiptPdfRenderer;
import br.com.sicape.api.application.attendance.receipt.ReceiptSnapshot;
import br.com.sicape.api.application.common.media.MediaContent;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ReceiptPdfRendererImpl implements ReceiptPdfRenderer {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
        .ofPattern("dd 'de' MMMM 'de' yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
        .withZone(ZoneId.of("America/Sao_Paulo"));

    private final String template;
    private final String logoDataUri;
    private final String logoSha256;

    /*
     * Template e Logo são únicos e obtidos através de resources neste momento.
     * Futuramente deve ser implementado uma entidade responsável por guardar as configurações da comarca.
     * Presentemente eles são carregados neste construtor por praticidade.
     */
    public ReceiptPdfRendererImpl() throws IOException {
        byte[] templateBytes;
        try (var stream = new ClassPathResource("receipt/receipt-template.xhtml").getInputStream()) {
            templateBytes = stream.readAllBytes();
        }
        this.template = new String(templateBytes, StandardCharsets.UTF_8);
        byte[] logo;
        try (var stream = new ClassPathResource("receipt/logo-template.png").getInputStream()) {
            logo = stream.readAllBytes();
        }
        this.logoDataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(logo);
        try {
            this.logoSha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(logo));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 indisponível", failure);
        }
    }

    @Override
    public String template() {
        return template;
    }

    @Override
    public String logoSha256() {
        return logoSha256;
    }

    @Override
    public byte[] render(ReceiptSnapshot snapshot, MediaContent convictedPhoto, MediaContent attendancePhoto) {
        String convictedPhotoDataUri = imageDataUri(snapshot.attendanceId().toString(),
            "cadastro", convictedPhoto);
        String attendancePhotoDataUri = imageDataUri(snapshot.attendanceId().toString(),
            "presença", attendancePhoto);
        String html = snapshot.templateXhtml();
        html = field(html, "processNumber", snapshot.processNumber());
        html = field(html, "convictedName", snapshot.convictedName());
        html = field(html, "convictedCpf", snapshot.convictedCpf());
        html = field(html, "districtName", snapshot.districtName());
        html = field(html, "courtName", snapshot.courtName());
        html = field(html, "operatorName", snapshot.operatorName());
        html = field(html, "protocol", snapshot.protocol());
        html = field(html, "phone", snapshot.phone());
        html = field(html, "address", snapshot.address());
        html = field(html, "employmentStatus", snapshot.employmentStatus());
        html = field(html, "attendedAt", DATE_TIME.format(snapshot.attendedAt()));
        html = field(html, "logoDataUri", logoDataUri);
        html = field(html, "convictedPhotoDataUri", convictedPhotoDataUri);
        html = field(html, "attendancePhotoDataUri", attendancePhotoDataUri);
        try {
            var output = new ByteArrayOutputStream();
            var builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception failure) {
            log.error("Falha ao renderizar comprovante; attendanceUuid={}", snapshot.attendanceId(), failure);
            throw new IllegalStateException("Não foi possível gerar o PDF do comprovante.", failure);
        }
    }

    private static String field(String html, String name, String value) {
        return html.replace("{{" + name + "}}", escape(value));
    }

    private static String imageDataUri(String attendanceUuid, String kind, MediaContent photo) {
        if (photo == null || photo.bytes() == null || photo.bytes().length == 0
            || !("image/jpeg".equals(photo.contentType()) || "image/png".equals(photo.contentType()))) {
            log.error("Foto inválida para comprovante; attendanceUuid={}, kind={}", attendanceUuid, kind);
            throw new IllegalStateException("A foto necessária ao comprovante é inválida.");
        }
        try {
            if (ImageIO.read(new ByteArrayInputStream(photo.bytes())) == null) {
                log.error("Imagem não decodificável para comprovante; attendanceUuid={}, kind={}",
                    attendanceUuid, kind);
                throw new IllegalStateException("A foto necessária ao comprovante é inválida.");
            }
        } catch (IOException failure) {
            log.error("Falha ao ler imagem do comprovante; attendanceUuid={}, kind={}",
                attendanceUuid, kind, failure);
            throw new IllegalStateException("A foto necessária ao comprovante é inválida.", failure);
        }
        return "data:" + photo.contentType() + ";base64,"
            + Base64.getEncoder().encodeToString(photo.bytes());
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
