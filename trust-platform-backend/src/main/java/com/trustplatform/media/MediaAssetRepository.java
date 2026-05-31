package com.trustplatform.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    List<MediaAsset> findByOwnerTypeAndOwnerIdOrderByOrderIndexAscIdAsc(String ownerType, Long ownerId);
    List<MediaAsset> findByOwnerTypeOrderByOrderIndexAscIdAsc(String ownerType);
    void deleteByOwnerTypeAndOwnerId(String ownerType, Long ownerId);
}
