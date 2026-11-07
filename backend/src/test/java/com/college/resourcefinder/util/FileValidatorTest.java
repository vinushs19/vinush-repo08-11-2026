package com.college.resourcefinder.util;

import com.college.resourcefinder.config.StorageProperties;
import com.college.resourcefinder.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class FileValidatorTest {

    private FileValidator validator;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setAllowedExtensions("pdf,docx,pptx,txt,xlsx,csv");
        properties.setMaxFileSizeMb(10);
        properties.setUploadDir("./target/test-storage");
        validator = new FileValidator(properties);
    }

    @Test
    @DisplayName("Valid PDF with %PDF header should pass validation")
    void shouldValidateValidPdfFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lecture_notes.pdf",
                "application/pdf",
                "%PDF-1.5 Valid PDF document content".getBytes()
        );
        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    @DisplayName("Valid DOCX with PK signature should pass validation")
    void shouldValidateValidDocxFile() {
        byte[] docxBytes = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "assignment.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes
        );
        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    @DisplayName("Corrupted or spoofed PDF without %PDF header should be rejected")
    void shouldRejectCorruptedPdfSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                "This is not a real PDF file".getBytes()
        );
        StorageException ex = assertThrows(StorageException.class, () -> validator.validate(file));
        assertTrue(ex.getMessage().contains("File signature verification failed"));
    }

    @Test
    @DisplayName("Corrupted or spoofed DOCX without PK header should be rejected")
    void shouldRejectCorruptedDocxSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "Random bytes not zip".getBytes()
        );
        StorageException ex = assertThrows(StorageException.class, () -> validator.validate(file));
        assertTrue(ex.getMessage().contains("File signature verification failed"));
    }

    @Test
    @DisplayName("MIME type mismatch should throw StorageException")
    void shouldRejectMimeMismatch() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.pdf",
                "image/png", // mismatched declared MIME
                "%PDF-1.4 PDF content".getBytes()
        );
        StorageException ex = assertThrows(StorageException.class, () -> validator.validate(file));
        assertTrue(ex.getMessage().contains("MIME type mismatch"));
    }

    @Test
    @DisplayName("Empty or null file should throw StorageException")
    void shouldRejectEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );
        StorageException ex = assertThrows(StorageException.class, () -> validator.validate(emptyFile));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("Disallowed file extension (.exe) should throw StorageException")
    void shouldRejectDisallowedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "virus_payload.exe",
                "application/octet-stream",
                "Executable content".getBytes()
        );
        StorageException ex = assertThrows(StorageException.class, () -> validator.validate(file));
        assertTrue(ex.getMessage().contains("not permitted"));
    }

    @Test
    @DisplayName("Filename with path traversal sequence should throw StorageException")
    void shouldRejectPathTraversal() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../etc/passwd.pdf",
                "application/pdf",
                "%PDF-1.4 Content".getBytes()
        );
        StorageException ex = assertThrows(StorageException.class, () -> validator.validate(file));
        assertTrue(ex.getMessage().contains("path traversal"));
    }

    @Test
    @DisplayName("File exceeding max size should throw StorageException")
    void shouldRejectOversizedFile() {
        byte[] oversized = new byte[11 * 1024 * 1024]; // 11 MB > 10 MB limit
        oversized[0] = 0x25; // %
        oversized[1] = 0x50; // P
        oversized[2] = 0x44; // D
        oversized[3] = 0x46; // F
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large_book.pdf",
                "application/pdf",
                oversized
        );
        StorageException ex = assertThrows(StorageException.class, () -> validator.validate(file));
        assertTrue(ex.getMessage().contains("exceeds the maximum allowed size"));
    }

    @Test
    @DisplayName("Sanitize should replace special characters with underscores")
    void shouldSanitizeUnsafeFilenames() {
        String sanitized = validator.sanitize("My Syllabus @ 2026 #1.pdf");
        assertEquals("My_Syllabus___2026__1.pdf", sanitized);
    }
}
