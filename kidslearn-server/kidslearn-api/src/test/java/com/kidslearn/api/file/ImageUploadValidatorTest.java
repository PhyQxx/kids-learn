package com.kidslearn.api.file;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.kidslearn.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageUploadValidatorTest {

    @Test
    void acceptsSupportedImageTypes() {
        assertDoesNotThrow(() -> ImageUploadValidator.validate(
            new MockMultipartFile("file", "question.png", "image/png", new byte[] {1})
        ));
        assertDoesNotThrow(() -> ImageUploadValidator.validate(
            new MockMultipartFile("file", "question.jpeg", "image/jpeg", new byte[] {1})
        ));
        assertDoesNotThrow(() -> ImageUploadValidator.validate(
            new MockMultipartFile("file", "question.webp", "image/webp", new byte[] {1})
        ));
    }

    @Test
    void rejectsNonImageContentType() {
        assertThrows(BusinessException.class, () -> ImageUploadValidator.validate(
            new MockMultipartFile("file", "question.png", "text/plain", new byte[] {1})
        ));
    }

    @Test
    void rejectsUnsupportedExtensionEvenWhenContentTypeLooksLikeImage() {
        assertThrows(BusinessException.class, () -> ImageUploadValidator.validate(
            new MockMultipartFile("file", "question.exe", "image/png", new byte[] {1})
        ));
    }

    @Test
    void rejectsEmptyFile() {
        assertThrows(BusinessException.class, () -> ImageUploadValidator.validate(
            new MockMultipartFile("file", "question.png", "image/png", new byte[0])
        ));
    }
}
