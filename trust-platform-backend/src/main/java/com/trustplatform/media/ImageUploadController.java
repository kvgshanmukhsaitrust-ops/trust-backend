package com.trustplatform.media;

import com.trustplatform.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class ImageUploadController {

    private final ImageOptimizer imageOptimizer;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Uploaded file is empty.", 400)
            );
        }

        try {
            // Save images to local uploads folder
            String uploadDir = "uploads";
            String url = imageOptimizer.optimizeAndSave(file, uploadDir);

            log.info("Successfully optimized and saved uploaded file to URL: {}", url);
            return ResponseEntity.ok(
                    ApiResponse.success("Image uploaded and optimized successfully.", Map.of("url", url))
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid image upload attempt: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage(), 400)
            );
        } catch (Exception e) {
            log.error("Failed to upload and optimize image", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Failed to upload image: " + e.getMessage(), 500)
            );
        }
    }
}
