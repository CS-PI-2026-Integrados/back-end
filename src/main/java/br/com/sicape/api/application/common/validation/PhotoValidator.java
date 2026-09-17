package br.com.sicape.api.application.common.validation;

import java.util.Locale;

import org.springframework.stereotype.Component;

import br.com.sicape.api.domain.exception.PayloadTooLargeException;
import br.com.sicape.api.domain.exception.ValidationException;

@Component
public class PhotoValidator {
    private static final long MAX_SIZE = 5L * 1024L * 1024L;

    public String validate(byte[] content, String declaredContentType) {
        if (content == null || content.length == 0) {
            throw new ValidationException("photo", "A foto é obrigatória");
        }
        if (content.length > MAX_SIZE) {
            throw new PayloadTooLargeException("A foto deve possuir no máximo 5 MB.");
        }

        String normalizedDeclaredType = normalizedDeclaredType(declaredContentType);
        String detectedContentType = detectedContentType(content);
        if (detectedContentType == null) {
            throw new ValidationException("photo", "A foto deve estar no formato JPG ou PNG");
        }
        if (!detectedContentType.equals(normalizedDeclaredType)) {
            throw new ValidationException("photo", "O tipo MIME não corresponde ao conteúdo da foto");
        }
        return detectedContentType;
    }

    private static String normalizedDeclaredType(String declaredContentType) {
        if (declaredContentType == null || declaredContentType.isBlank()) {
            throw new ValidationException("photo", "Informe o tipo MIME image/jpeg ou image/png");
        }
        String normalized = declaredContentType.toLowerCase(Locale.ROOT);
        if (!normalized.matches("image/(jpeg|png)")) {
            throw new ValidationException("photo", "Informe o tipo MIME image/jpeg ou image/png");
        }
        return normalized;
    }

    private static String detectedContentType(byte[] content) {
        if (isJpeg(content)) {
            return "image/jpeg";
        }
        return isPng(content) ? "image/png" : null;
    }

    private static boolean isJpeg(byte[] content) {
        return content.length >= 3
            && (content[0] & 0xff) == 0xff
            && (content[1] & 0xff) == 0xd8
            && (content[2] & 0xff) == 0xff;
    }

    private static boolean isPng(byte[] content) {
        int[] signature = {0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if ((content[index] & 0xff) != signature[index]) {
                return false;
            }
        }
        return true;
    }
}
