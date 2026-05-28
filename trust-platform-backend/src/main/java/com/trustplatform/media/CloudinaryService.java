package com.trustplatform.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private Cloudinary cloudinary;
    private final ImageOptimizer imageOptimizer;

    public boolean isConfigured() {
        return cloudinary != null;
    }

    /**
     * Uploads media to Cloudinary or falls back to local storage.
     * Returns metadata containing: secure_url, public_id, width, height, aspect_ratio, duration, and media_type.
     */
    @SuppressWarnings("rawtypes")
    public Map<String, Object> uploadMedia(MultipartFile file, String mediaTypeStr) throws IOException {
        String resourceType = "VIDEO".equalsIgnoreCase(mediaTypeStr) ? "video" : "image";

        if (isConfigured()) {
            try {
                log.info("Cloudinary is active. Uploading {} file: {}", resourceType, file.getOriginalFilename());
                Map params = ObjectUtils.asMap(
                        "resource_type", resourceType,
                        "folder", "trust_platform"
                );

                Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("secure_url", uploadResult.get("secure_url"));
                metadata.put("public_id", uploadResult.get("public_id"));
                
                Integer width = (Integer) uploadResult.get("width");
                Integer height = (Integer) uploadResult.get("height");
                metadata.put("width", width);
                metadata.put("height", height);

                Double aspectRatio = null;
                if (uploadResult.containsKey("aspect_ratio")) {
                    Object ar = uploadResult.get("aspect_ratio");
                    if (ar instanceof Number) {
                        aspectRatio = ((Number) ar).doubleValue();
                    }
                }
                if (aspectRatio == null && width != null && height != null && height > 0) {
                    aspectRatio = (double) width / height;
                }
                metadata.put("aspect_ratio", aspectRatio);

                Double duration = null;
                if (uploadResult.containsKey("duration")) {
                    Object dur = uploadResult.get("duration");
                    if (dur instanceof Number) {
                        duration = ((Number) dur).doubleValue();
                    }
                }
                metadata.put("duration", duration);
                metadata.put("media_type", "video".equals(resourceType) ? "VIDEO" : "IMAGE");

                log.info("Cloudinary upload successful. public_id: {}, url: {}", metadata.get("public_id"), metadata.get("secure_url"));
                return metadata;
            } catch (Exception e) {
                log.error("Cloudinary upload failed for {}. Falling back to local storage...", file.getOriginalFilename(), e);
                // Fallthrough to local upload fallback if Cloudinary has runtime error
            }
        }

        log.warn("Cloudinary not configured or failed. Falling back to local storage for: {}", file.getOriginalFilename());
        return uploadLocalFallback(file, mediaTypeStr);
    }

    private Map<String, Object> uploadLocalFallback(MultipartFile file, String mediaTypeStr) throws IOException {
        String uploadDir = "uploads";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Map<String, Object> metadata = new HashMap<>();
        String uniqueId = UUID.randomUUID().toString();
        
        if ("VIDEO".equalsIgnoreCase(mediaTypeStr)) {
            // Save local video
            String extension = "mp4";
            String originalName = file.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf(".") + 1);
            }
            String filename = uniqueId + "." + extension;
            File targetFile = new File(dir, filename);
            
            Files.copy(file.getInputStream(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            String localUrl = "/uploads/" + filename;
            metadata.put("secure_url", localUrl);
            metadata.put("public_id", "local_vid_" + uniqueId);
            metadata.put("width", 1920);
            metadata.put("height", 1080);
            metadata.put("aspect_ratio", 16.0 / 9.0);
            metadata.put("duration", 12.5); // Simulated duration
            metadata.put("media_type", "VIDEO");
            
            log.info("Saved local fallback video to URL: {}", localUrl);
        } else {
            // Optimize and save image locally
            String localUrl = imageOptimizer.optimizeAndSave(file, uploadDir);
            
            // Read image dimensions for accurate metadata
            int width = 800;
            int height = 600;
            try {
                BufferedImage img = ImageIO.read(file.getInputStream());
                if (img != null) {
                    width = img.getWidth();
                    height = img.getHeight();
                }
            } catch (Exception e) {
                log.warn("Could not read local image dimensions, using defaults. Error: {}", e.getMessage());
            }

            metadata.put("secure_url", localUrl);
            metadata.put("public_id", "local_img_" + uniqueId);
            metadata.put("width", width);
            metadata.put("height", height);
            metadata.put("aspect_ratio", (double) width / height);
            metadata.put("duration", null);
            metadata.put("media_type", "IMAGE");
            
            log.info("Saved local fallback image to URL: {} (width: {}, height: {})", localUrl, width, height);
        }
        
        return metadata;
    }
}
