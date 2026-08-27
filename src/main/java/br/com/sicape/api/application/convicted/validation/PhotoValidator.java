package br.com.sicape.api.application.convicted.validation;

import java.util.Locale;

import org.springframework.stereotype.Component;

import br.com.sicape.api.domain.exception.PayloadTooLargeException;
import br.com.sicape.api.domain.exception.ValidationException;

@Component
public class PhotoValidator {
    private static final long MAX_SIZE = 5L * 1024L * 1024L;

    public void validate(byte[] content, String declaredContentType) {
        if (content == null || content.length == 0) {
            throw new ValidationException("photo", "A foto é obrigatória");
        }
        if (content.length > MAX_SIZE) {
            throw new PayloadTooLargeException("A foto deve possuir no máximo 5 MB.");
        }

        String detectedContentType = isJpeg(content)
            ? "image/jpeg"
            : isPng(content) ? "image/png" : null;
        if (detectedContentType == null) {
            throw new ValidationException("photo", "A foto deve estar no formato JPG ou PNG");
        }

        if (declaredContentType != null && !declaredContentType.isBlank()) {
            String normalizedDeclared = declaredContentType.equalsIgnoreCase("image/jpg")
                ? "image/jpeg"
                : declaredContentType.toLowerCase(Locale.ROOT);
            if (!detectedContentType.equals(normalizedDeclared)) {
                throw new ValidationException("photo", "O tipo MIME não corresponde ao conteúdo da foto");
            }
        }
    }

    private boolean isJpeg(byte[] content) {
        return content.length >= 3
            && (content[0] & 0xff) == 0xff
            && (content[1] & 0xff) == 0xd8
            && (content[2] & 0xff) == 0xff;
    }

    private boolean isPng(byte[] content) {
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
