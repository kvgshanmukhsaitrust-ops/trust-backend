package com.trustplatform.media;

import com.trustplatform.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaAssetController {

    private final MediaAssetRepository mediaAssetRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MediaAsset>>> getMediaAssets(
            @RequestParam(value = "ownerType", defaultValue = "GENERAL") String ownerType) {
        log.info("Fetching media assets for ownerType: {}", ownerType);
        
        List<MediaAsset> assets;
        if ("ALL".equalsIgnoreCase(ownerType)) {
            assets = mediaAssetRepository.findAll();
        } else {
            assets = mediaAssetRepository.findByOwnerTypeOrderByOrderIndexAscIdAsc(ownerType);
        }
        
        return ResponseEntity.ok(ApiResponse.success("Media assets retrieved successfully.", assets));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_MEDIA')")
    public ResponseEntity<ApiResponse<MediaAsset>> createMediaAsset(@RequestBody MediaAsset mediaAsset) {
        log.info("Saving new media asset mapping: {}", mediaAsset);
        
        if (mediaAsset.getOwnerId() == null) {
            mediaAsset.setOwnerId(0L); // Default ownerId
        }
        if (mediaAsset.getOwnerType() == null) {
            mediaAsset.setOwnerType("GENERAL");
        }
        
        MediaAsset saved = mediaAssetRepository.save(mediaAsset);
        return ResponseEntity.ok(ApiResponse.success("Media asset saved successfully.", saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_MEDIA')")
    public ResponseEntity<ApiResponse<Void>> deleteMediaAsset(@PathVariable Long id) {
        log.info("Deleting media asset ID: {}", id);
        
        if (!mediaAssetRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        mediaAssetRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Media asset mapping deleted successfully.", null));
    }
}
