package br.com.sicape.api.infrastructure.receipt;

import static org.assertj.core.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import br.com.sicape.api.application.attendance.receipt.ReceiptSnapshot;
import br.com.sicape.api.application.common.media.MediaContent;

class ReceiptPdfRendererTest {
    @Test
    void rendersTemplateWithAttendanceDistrictAndPhoto() throws Exception {
        var renderer = new ReceiptPdfRendererImpl();
        var image = new BufferedImage(240, 320, BufferedImage.TYPE_INT_RGB);
        var presenceGraphics = image.createGraphics();
        presenceGraphics.setColor(new java.awt.Color(0x112233));
        presenceGraphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        presenceGraphics.dispose();
        var imageBytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", imageBytes);
        var convictedImage = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        var cadastralGraphics = convictedImage.createGraphics();
        cadastralGraphics.setColor(new java.awt.Color(0x445566));
        cadastralGraphics.fillRect(0, 0, convictedImage.getWidth(), convictedImage.getHeight());
        cadastralGraphics.dispose();
        var convictedImageBytes = new ByteArrayOutputStream();
        ImageIO.write(convictedImage, "png", convictedImageBytes);
        var snapshot = new ReceiptSnapshot(UUID.randomUUID(), Instant.parse("2026-09-25T22:42:00Z"),
            "Rua Central, 10 - Centro, Cidade/SP - CEP 12345678", "(11) 98888-1001", "Trabalho formal",
            "Arthur Morgan", "529.982.247-25", "0001234-56.2026.8.26.0001", "Comarca Central",
            "Vara Única de Teste", "Operador", "PROTOCOLO-123", renderer.template(), renderer.logoSha256());

        byte[] pdf = renderer.render(snapshot,
            new MediaContent(convictedImageBytes.toByteArray(), "image/png"),
            new MediaContent(imageBytes.toByteArray(), "image/png"));

        assertThat(pdf).startsWith("%PDF".getBytes());
        try (var document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            var text = new PDFTextStripper().getText(document);
            assertThat(text).contains("COMPROVANTE DE COMPARECIMENTO", "Arthur Morgan",
                "Comarca Central", "Vara Única de Teste", "PROTOCOLO-123", "Trabalho formal",
                "529.982.247-25", "(11) 98888-1001");
            assertThat(document.getPage(0).getResources().getXObjectNames()).hasSizeGreaterThanOrEqualTo(3);
            int framedPhotos = 0;
            var resources = document.getPage(0).getResources();
            for (var name : resources.getXObjectNames()) {
                if (resources.getXObject(name) instanceof PDImageXObject photo
                    && photo.getWidth() == 300 && photo.getHeight() == 300) {
                    framedPhotos++;
                    var pixels = photo.getImage();
                    assertThat(pixels.getRGB(0, 0) & 0xffffff).isEqualTo(0xffffff);
                    assertThat(pixels.getRGB(150, 150) & 0xffffff).isIn(0x112233, 0x445566);
                }
            }
            assertThat(framedPhotos).isEqualTo(2);
            if (Boolean.getBoolean("receipt.preview")) {
                Files.write(Path.of("target/receipt-photos-preview.pdf"), pdf);
                ImageIO.write(new PDFRenderer(document).renderImageWithDPI(0, 120), "png",
                    Path.of("target/receipt-photos-preview.png").toFile());
            }
        }
    }
}
