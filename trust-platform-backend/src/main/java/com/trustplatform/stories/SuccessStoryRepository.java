package com.trustplatform.stories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SuccessStoryRepository extends JpaRepository<SuccessStory, Long> {
    List<SuccessStory> findByPublishedTrueOrderByDisplayOrderAscIdDesc();
}