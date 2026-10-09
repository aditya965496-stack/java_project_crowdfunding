package com.crowdfund.service;

import com.crowdfund.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/**
 * Production-ready file upload service.
 * Enforces strict MIME checking, magic-byte inspection, file size bounds,
 * and path-traversal immunization.
 */
@Service
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);

    @Value("${crowdfund.upload.dir:./uploads}")
    private String uploadBaseDir;

    @Value("${crowdfund.upload.max-size-bytes:5242880}") // 5 MB
    private long maxFileSizeBytes;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    // Magic numbers
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] RIFF_MAGIC = new byte[]{0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MAGIC = new byte[]{0x57, 0x45, 0x42, 0x50};

    /**
     * Stores an uploaded campaign image securely and returns the web-accessible URL path.
     */
    public String storeCampaignImage(InputStream inputStream, String originalFilename,
                                    String reportedMimeType, long contentLength) throws IOException {

        if (inputStream == null) {
            throw new BusinessRuleException("Upload stream is empty.");
        }

        if (contentLength > maxFileSizeBytes) {
            throw new BusinessRuleException("File size exceeds maximum permitted limit of " + (maxFileSizeBytes / (1024 * 1024)) + " MB.");
        }

        // Validate reported MIME type
        if (reportedMimeType == null || !ALLOWED_MIME_TYPES.contains(reportedMimeType.toLowerCase())) {
            throw new BusinessRuleException("Invalid file type: " + reportedMimeType + ". Only JPEG, PNG, and WebP images are allowed.");
        }

        // Read stream into memory or buffer to inspect magic bytes & enforce max size
        byte[] fileBytes = inputStream.readNBytes((int) maxFileSizeBytes + 1);
        if (fileBytes.length > maxFileSizeBytes) {
            throw new BusinessRuleException("File size exceeds maximum limit of " + (maxFileSizeBytes / (1024 * 1024)) + " MB.");
        }

        // Validate file signature (magic bytes)
        String detectedExtension = detectAndValidateExtension(fileBytes, originalFilename);

        // Ensure upload directory exists
        Path targetDir = Paths.get(uploadBaseDir, "campaigns").toAbsolutePath().normalize();
        Files.createDirectories(targetDir);

        // Generate sanitized UUID filename (immune to path traversal like ../../etc/passwd)
        String safeFileName = UUID.randomUUID() + "." + detectedExtension;
        Path destination = targetDir.resolve(safeFileName).normalize();

        // Extra guard: Ensure destination is strictly within targetDir
        if (!destination.startsWith(targetDir)) {
            throw new BusinessRuleException("Path traversal attempt detected.");
        }

        Files.copy(new ByteArrayInputStream(fileBytes), destination, StandardCopyOption.REPLACE_EXISTING);
        log.info("Securely stored uploaded image: {}", safeFileName);

        return "/uploads/campaigns/" + safeFileName;
    }

    /**
     * Stores an uploaded payment proof screenshot securely and returns the web-accessible URL path.
     */
    public String storePaymentScreenshot(InputStream inputStream, String originalFilename,
                                         String reportedMimeType, long contentLength) throws IOException {
        if (inputStream == null) {
            throw new BusinessRuleException("Payment screenshot upload stream is empty.");
        }

        if (contentLength > maxFileSizeBytes) {
            throw new BusinessRuleException("File size exceeds maximum permitted limit of " + (maxFileSizeBytes / (1024 * 1024)) + " MB.");
        }

        if (reportedMimeType == null || !ALLOWED_MIME_TYPES.contains(reportedMimeType.toLowerCase())) {
            throw new BusinessRuleException("Invalid file type: " + reportedMimeType + ". Only JPEG, PNG, and WebP images are allowed.");
        }

        byte[] fileBytes = inputStream.readNBytes((int) maxFileSizeBytes + 1);
        if (fileBytes.length > maxFileSizeBytes) {
            throw new BusinessRuleException("File size exceeds maximum limit of " + (maxFileSizeBytes / (1024 * 1024)) + " MB.");
        }

        String detectedExtension = detectAndValidateExtension(fileBytes, originalFilename);

        Path targetDir = Paths.get(uploadBaseDir, "payments").toAbsolutePath().normalize();
        Files.createDirectories(targetDir);

        String safeFileName = "upi_" + UUID.randomUUID() + "." + detectedExtension;
        Path destination = targetDir.resolve(safeFileName).normalize();

        if (!destination.startsWith(targetDir)) {
            throw new BusinessRuleException("Path traversal attempt detected.");
        }

        Files.copy(new ByteArrayInputStream(fileBytes), destination, StandardCopyOption.REPLACE_EXISTING);
        log.info("Securely stored UPI payment screenshot: {}", safeFileName);

        return "/uploads/payments/" + safeFileName;
    }

    private String detectAndValidateExtension(byte[] bytes, String originalFilename) {
        if (bytes == null || bytes.length < 12) {
            throw new BusinessRuleException("File is empty or too small to be a valid image.");
        }

        // Check JPEG
        if (bytes[0] == JPEG_MAGIC[0] && bytes[1] == JPEG_MAGIC[1] && bytes[2] == JPEG_MAGIC[2]) {
            return "jpg";
        }

        // Check PNG
        if (bytes[0] == PNG_MAGIC[0] && bytes[1] == PNG_MAGIC[1] &&
                bytes[2] == PNG_MAGIC[2] && bytes[3] == PNG_MAGIC[3]) {
            return "png";
        }

        // Check WebP (RIFF....WEBP)
        if (bytes[0] == RIFF_MAGIC[0] && bytes[1] == RIFF_MAGIC[1] &&
                bytes[2] == RIFF_MAGIC[2] && bytes[3] == RIFF_MAGIC[3] &&
                bytes[8] == WEBP_MAGIC[0] && bytes[9] == WEBP_MAGIC[1] &&
                bytes[10] == WEBP_MAGIC[2] && bytes[11] == WEBP_MAGIC[3]) {
            return "webp";
        }

        throw new BusinessRuleException("File header does not match a valid JPEG, PNG, or WebP image signature.");
    }
}
