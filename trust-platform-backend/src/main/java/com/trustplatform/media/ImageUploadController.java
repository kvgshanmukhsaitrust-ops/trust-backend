package com.trustplatform.media;

import com.trustplatform.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.trustplatform.audit.AuditAction;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class ImageUploadController {

    private final CloudinaryService cloudinaryService;

    // Supported lists of MIME types
    private static final List<String> SUPPORTED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/jpg", "application/pdf"
    );

    private static final List<String> SUPPORTED_VIDEO_TYPES = List.of(
            "video/mp4", "video/webm", "video/ogg", "video/quicktime"
    );

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('MANAGE_MEDIA')")
    @AuditAction("UPLOAD_MEDIA")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mediaType", defaultValue = "IMAGE") String mediaType) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Uploaded file is empty.", 400)
            );
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            log.error("Failed to read uploaded file bytes", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Failed to read file bytes: " + e.getMessage(), 500)
            );
        }

        MultipartFile repeatableFile = new ByteArrayMultipartFile(
                fileBytes,
                file.getName(),
                file.getOriginalFilename(),
                file.getContentType()
        );

        // Validate formats
        String contentType = repeatableFile.getContentType();
        if (contentType == null) {
            contentType = "";
        }

        log.info("Validating upload of file: {}, contentType: {}, mediaType: {}", 
                repeatableFile.getOriginalFilename(), contentType, mediaType);

        if ("VIDEO".equalsIgnoreCase(mediaType)) {
            if (!contentType.startsWith("video/") && !SUPPORTED_VIDEO_TYPES.contains(contentType.toLowerCase())) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Unsupported video format. Allowed: MP4, WebM, OGG, QuickTime.", 400)
                );
            }
        } else {
            // Default to IMAGE validation
            if (!contentType.startsWith("image/") && !"application/pdf".equalsIgnoreCase(contentType) && !SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Unsupported image/document format. Allowed: JPEG, PNG, PDF.", 400)
                );
            }
        }

        // Validate Magic Bytes
        if (!validateMagicBytes(repeatableFile, mediaType)) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("File signature verification failed. The file content does not match its declared format.", 400)
            );
        }

        // Validate size (10MB image limit, 50MB is capped by Spring Boot's MaxUploadSizeExceededException but we can add safety checks here too)
        if (repeatableFile.getSize() > 10 * 1024 * 1024 && !"VIDEO".equalsIgnoreCase(mediaType)) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Image file size exceeds the 10MB limit.", 400)
            );
        }

        try {
            // Upload to Cloudinary (or local fallback)
            Map<String, Object> uploadResult = cloudinaryService.uploadMedia(repeatableFile, mediaType);

            // Maintain backward compatibility with the old field "url"
            uploadResult.put("url", uploadResult.get("secure_url"));

            log.info("Successfully uploaded media. Metadata: {}", uploadResult);
            return ResponseEntity.ok(
                    ApiResponse.success("Media uploaded and optimized successfully.", uploadResult)
            );
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            log.error("Invalid media upload attempt: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage(), 400)
            );
        } catch (Exception e) {
            log.error("Failed to upload and optimize media", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Media upload failed. Please try again with a valid image file.", 500)
            );
        }
    }

    private boolean validateMagicBytes(MultipartFile file, String mediaType) {
        try (java.io.InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int bytesRead = is.read(header);
            if (bytesRead < 4) {
                return false;
            }

            // Check JPEG (FF D8 FF)
            if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
                return "IMAGE".equalsIgnoreCase(mediaType);
            }

            // Check PNG (89 50 4E 47)
            if (header[0] == (byte) 0x89 && header[1] == (byte) 0x50 && header[2] == (byte) 0x4E && header[3] == (byte) 0x47) {
                return "IMAGE".equalsIgnoreCase(mediaType);
            }

            // Check PDF (%PDF)
            if (header[0] == (byte) 0x25 && header[1] == (byte) 0x50 && header[2] == (byte) 0x44 && header[3] == (byte) 0x46) {
                return "IMAGE".equalsIgnoreCase(mediaType);
            }

            // Check WebM (1A 45 DF A3)
            if (header[0] == (byte) 0x1A && header[1] == (byte) 0x45 && header[2] == (byte) 0xDF && header[3] == (byte) 0xA3) {
                return "VIDEO".equalsIgnoreCase(mediaType);
            }

            // Check MP4 (ftyp)
            if (bytesRead >= 8 && header[4] == (byte) 0x66 && header[5] == (byte) 0x74 && header[6] == (byte) 0x79 && header[7] == (byte) 0x70) {
                return "VIDEO".equalsIgnoreCase(mediaType);
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
