package com.trustplatform.stories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StoryTimelineMilestoneRepository extends JpaRepository<StoryTimelineMilestone, Long> {
    List<StoryTimelineMilestone> findByStoryIdOrderByOrderIndexAscIdAsc(Long storyId);
    void deleteByStoryId(Long storyId);
}
