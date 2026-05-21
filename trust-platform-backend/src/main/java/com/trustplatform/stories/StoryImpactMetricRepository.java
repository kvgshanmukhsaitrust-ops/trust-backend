package com.trustplatform.stories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StoryImpactMetricRepository extends JpaRepository<StoryImpactMetric, Long> {
    List<StoryImpactMetric> findByStoryIdOrderByDisplayOrderAscIdAsc(Long storyId);
    void deleteByStoryId(Long storyId);
}
