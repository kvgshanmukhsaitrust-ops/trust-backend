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
            "image/jpeg", "image/png", "image/webp", "image/gif", "image/svg+xml", "image/jpg"
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

        // Validate formats
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "";
        }

        log.info("Validating upload of file: {}, contentType: {}, mediaType: {}", 
                file.getOriginalFilename(), contentType, mediaType);

        if ("VIDEO".equalsIgnoreCase(mediaType)) {
            if (!contentType.startsWith("video/") && !SUPPORTED_VIDEO_TYPES.contains(contentType.toLowerCase())) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Unsupported video format. Allowed: MP4, WebM, OGG, QuickTime.", 400)
                );
            }
        } else {
            // Default to IMAGE validation
            if (!contentType.startsWith("image/") && !SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Unsupported image format. Allowed: JPEG, PNG, WEBP, GIF, SVG.", 400)
                );
            }
        }

        // Validate size (10MB image limit, 50MB is capped by Spring Boot's MaxUploadSizeExceededException but we can add safety checks here too)
        if (file.getSize() > 10 * 1024 * 1024 && !"VIDEO".equalsIgnoreCase(mediaType)) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Image file size exceeds the 10MB limit.", 400)
            );
        }

        try {
            // Upload to Cloudinary (or local fallback)
            Map<String, Object> uploadResult = cloudinaryService.uploadMedia(file, mediaType);

            // Maintain backward compatibility with the old field "url"
            uploadResult.put("url", uploadResult.get("secure_url"));

            log.info("Successfully uploaded media. Metadata: {}", uploadResult);
            return ResponseEntity.ok(
                    ApiResponse.success("Media uploaded and optimized successfully.", uploadResult)
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid media upload attempt: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage(), 400)
            );
        } catch (Exception e) {
            log.error("Failed to upload and optimize media", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Failed to upload media: " + e.getMessage(), 500)
            );
        }
    }
}
