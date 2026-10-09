package com.crowdfund.service;

import com.crowdfund.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUploadServiceTest {

    private FileUploadService fileUploadService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileUploadService = new FileUploadService();
        ReflectionTestUtils.setField(fileUploadService, "uploadBaseDir", tempDir.toString());
        ReflectionTestUtils.setField(fileUploadService, "maxFileSizeBytes", 5 * 1024 * 1024L);
    }

    @Test
    @DisplayName("should accept valid PNG image with correct magic bytes")
    void shouldAcceptValidPng() throws IOException {
        // PNG header: 89 50 4E 47 0D 0A 1A 0A followed by dummy data
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 1, 2, 3};
        String url = fileUploadService.storeCampaignImage(
                new ByteArrayInputStream(pngBytes),
                "photo.png",
                "image/png",
                pngBytes.length
        );

        assertNotNull(url);
        assertTrue(url.startsWith("/uploads/campaigns/"));
        assertTrue(url.endsWith(".png"));
    }

    @Test
    @DisplayName("should accept valid JPEG image with correct magic bytes")
    void shouldAcceptValidJpeg() throws IOException {
        // JPEG header: FF D8 FF E0 followed by dummy bytes
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 1, 2, 3, 4};
        String url = fileUploadService.storeCampaignImage(
                new ByteArrayInputStream(jpegBytes),
                "banner.jpg",
                "image/jpeg",
                jpegBytes.length
        );

        assertNotNull(url);
        assertTrue(url.startsWith("/uploads/campaigns/"));
        assertTrue(url.endsWith(".jpg"));
    }

    @Test
    @DisplayName("should reject file exceeding maximum size limit")
    void shouldRejectOversizedFile() {
        ReflectionTestUtils.setField(fileUploadService, "maxFileSizeBytes", 100L);
        byte[] largeBytes = new byte[150];

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                fileUploadService.storeCampaignImage(
                        new ByteArrayInputStream(largeBytes),
                        "huge.jpg",
                        "image/jpeg",
                        largeBytes.length
                )
        );
        assertTrue(ex.getMessage().contains("exceeds maximum"));
    }

    @Test
    @DisplayName("should reject disallowed MIME types like executable or script")
    void shouldRejectDisallowedMime() {
        byte[] bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                fileUploadService.storeCampaignImage(
                        new ByteArrayInputStream(bytes),
                        "script.sh",
                        "application/x-sh",
                        bytes.length
                )
        );
        assertTrue(ex.getMessage().contains("Invalid file type"));
    }

    @Test
    @DisplayName("should reject fake image where reported MIME is image/png but signature is corrupt")
    void shouldRejectCorruptMagicBytes() {
        byte[] fakeBytes = "This is a text file not a PNG".getBytes();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                fileUploadService.storeCampaignImage(
                        new ByteArrayInputStream(fakeBytes),
                        "fake.png",
                        "image/png",
                        fakeBytes.length
                )
        );
        assertTrue(ex.getMessage().contains("header does not match"));
    }

    @Test
    @DisplayName("should sanitize path traversal attempts in filename")
    void shouldSanitizePathTraversal() throws IOException {
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 1, 2, 3};
        String url = fileUploadService.storeCampaignImage(
                new ByteArrayInputStream(pngBytes),
                "../../../../etc/passwd.png",
                "image/png",
                pngBytes.length
        );

        assertNotNull(url);
        assertFalse(url.contains(".."));
        assertTrue(url.startsWith("/uploads/campaigns/"));
    }
}
