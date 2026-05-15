package com.trustplatform.setting;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PageContentRepository extends JpaRepository<PageContent, Long> {
    Optional<PageContent> findByContentKey(String contentKey);
}
