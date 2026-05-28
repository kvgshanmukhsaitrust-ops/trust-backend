package com.trustplatform.stories;

import com.trustplatform.stories.dto.SuccessStoryResponse;
import com.trustplatform.stories.dto.SuccessStorySummaryResponse;
import com.trustplatform.media.MediaAsset;
import com.trustplatform.media.MediaAssetRepository;
import com.trustplatform.event.media.MediaType;
import com.trustplatform.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.trustplatform.common.ContentVersion;
import com.trustplatform.common.ContentVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class SuccessStoryService {

    private final SuccessStoryRepository storyRepository;
    private final StoryTimelineMilestoneRepository milestoneRepository;
    private final StoryImpactMetricRepository metricRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final ContentVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<SuccessStorySummaryResponse> getAllStories(Boolean admin) {
        List<SuccessStory> stories;
        if (Boolean.TRUE.equals(admin)) {
            stories = storyRepository.findAll();
        } else {
            stories = storyRepository.findByPublishedTrueOrderByDisplayOrderAscIdDesc();
        }
        return stories.stream().map(this::mapToSummaryResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SuccessStoryResponse getStoryById(Long id) {
        SuccessStory story = storyRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Success story not found with id: " + id));
        return mapToResponse(story);
    }

    @Transactional
    public SuccessStoryResponse createStory(SuccessStory request) {
        SuccessStory story = new SuccessStory();
        copyStoryFields(request, story);
        
        // Save the story first
        SuccessStory savedStory = storyRepository.save(story);

        // Associate milestones if present
        if (request.getTimeline() != null && !request.getTimeline().isEmpty()) {
            for (StoryTimelineMilestone milestone : request.getTimeline()) {
                milestone.setStory(savedStory);
                savedStory.getTimeline().add(milestone);
            }
        }

        // Associate metrics if present
        if (request.getMetrics() != null && !request.getMetrics().isEmpty()) {
            for (StoryImpactMetric metric : request.getMetrics()) {
                metric.setStory(savedStory);
                savedStory.getMetrics().add(metric);
            }
        }

        SuccessStory finalized = storyRepository.save(savedStory);
        return mapToResponse(finalized);
    }

    @Transactional
    public SuccessStoryResponse updateStory(Long id, SuccessStory updated) {
        SuccessStory story = storyRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Success story not found with id: " + id));

        // Save version snapshot before update
        try {
            ContentVersion lastVersion = versionRepository.findTopByEntityTypeAndEntityIdOrderByVersionNumberDesc("STORY", id.toString());
            int nextVersion = lastVersion == null ? 1 : lastVersion.getVersionNumber() + 1;
            
            // To avoid Lazy initialization issues, we could just serialize the Response DTO as the snapshot since it contains all data
            SuccessStoryResponse snapshotDto = mapToResponse(story);
            String jsonSnapshot = objectMapper.writeValueAsString(snapshotDto);

            ContentVersion cv = ContentVersion.builder()
                .entityType("STORY")
                .entityId(id.toString())
                .contentSnapshot(jsonSnapshot)
                .versionNumber(nextVersion)
                .createdBy("ADMIN")
                .createdAt(java.time.LocalDateTime.now())
                .build();
            versionRepository.save(cv);
        } catch (Exception e) {
            System.err.println("Failed to snapshot story version: " + e.getMessage());
        }

        copyStoryFields(updated, story);

        // Update timeline (orphan removal will delete old ones not present in new list)
        story.getTimeline().clear();
        if (updated.getTimeline() != null) {
            for (StoryTimelineMilestone milestone : updated.getTimeline()) {
                milestone.setStory(story);
                story.getTimeline().add(milestone);
            }
        }

        // Update metrics
        story.getMetrics().clear();
        if (updated.getMetrics() != null) {
            for (StoryImpactMetric metric : updated.getMetrics()) {
                metric.setStory(story);
                story.getMetrics().add(metric);
            }
        }

        SuccessStory saved = storyRepository.save(story);
        return mapToResponse(saved);
    }

    @Transactional
    public SuccessStoryResponse rollbackStory(Long id, Long versionId) {
        ContentVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new ResourceNotFoundException("Version not found"));
        
        if (!"STORY".equals(version.getEntityType()) || !id.toString().equals(version.getEntityId())) {
            throw new IllegalArgumentException("Invalid version for this story");
        }

        try {
            // We saved the SuccessStoryResponse as JSON
            SuccessStoryResponse snapshot = objectMapper.readValue(version.getContentSnapshot(), SuccessStoryResponse.class);
            
            // Convert back to SuccessStory entity representation
            SuccessStory rollbackEntity = new SuccessStory();
            rollbackEntity.setTitle(snapshot.getTitle());
            rollbackEntity.setDescription(snapshot.getDescription());
            rollbackEntity.setImageUrl(snapshot.getImageUrl());
            rollbackEntity.setCategory(snapshot.getCategory());
            rollbackEntity.setPublished(snapshot.isPublished());
            rollbackEntity.setFeatured(snapshot.isFeatured());
            rollbackEntity.setDisplayOrder(snapshot.getDisplayOrder());
            rollbackEntity.setLocation(snapshot.getLocation());
            rollbackEntity.setSubtitle(snapshot.getSubtitle());
            rollbackEntity.setBeforeImageUrl(snapshot.getBeforeImageUrl());
            rollbackEntity.setAfterImageUrl(snapshot.getAfterImageUrl());
            rollbackEntity.setVideoUrl(snapshot.getVideoUrl());
            rollbackEntity.setTestimonialQuote(snapshot.getTestimonialQuote());
            rollbackEntity.setTestimonialAuthor(snapshot.getTestimonialAuthor());

            if (snapshot.getTimeline() != null) {
                rollbackEntity.setTimeline(snapshot.getTimeline().stream().map(m -> {
                    StoryTimelineMilestone sm = new StoryTimelineMilestone();
                    sm.setDate(m.getDate());
                    sm.setTitle(m.getTitle());
                    sm.setDescription(m.getDescription());
                    sm.setImageUrl(m.getImageUrl());
                    sm.setOrderIndex(m.getOrderIndex());
                    return sm;
                }).collect(Collectors.toList()));
            }

            if (snapshot.getMetrics() != null) {
                rollbackEntity.setMetrics(snapshot.getMetrics().stream().map(m -> {
                    StoryImpactMetric sim = new StoryImpactMetric();
                    sim.setLabel(m.getLabel());
                    sim.setValue(m.getValue());
                    sim.setIcon(m.getIcon());
                    sim.setDisplayOrder(m.getDisplayOrder());
                    return sim;
                }).collect(Collectors.toList()));
            }

            return updateStory(id, rollbackEntity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to rollback story: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<ContentVersion> getStoryVersions(Long storyId) {
        return versionRepository.findByEntityTypeAndEntityIdOrderByVersionNumberDesc("STORY", storyId.toString());
    }

    @Transactional
    public void deleteStory(Long id) {
        SuccessStory story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Success story not found with id: " + id));
        story.setDeleted(true);
        storyRepository.save(story);
    }

    @Transactional
    public SuccessStoryResponse togglePublish(Long id, boolean value) {
        SuccessStory story = storyRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Success story not found with id: " + id));
        story.setPublished(value);
        SuccessStory saved = storyRepository.save(story);
        return mapToResponse(saved);
    }

    @Transactional
    public void updateStoryGallery(Long storyId, List<MediaAsset> newGallery) {
        // Delete existing gallery assets
        mediaAssetRepository.deleteByOwnerTypeAndOwnerId("STORY", storyId);

        if (newGallery != null) {
            for (MediaAsset asset : newGallery) {
                asset.setOwnerType("STORY");
                asset.setOwnerId(storyId);
                mediaAssetRepository.save(asset);
            }
        }
    }

    @Transactional
    public void reorderStories(List<Long> storyIds) {
        for (int i = 0; i < storyIds.size(); i++) {
            Long id = storyIds.get(i);
            final int displayOrder = i;
            storyRepository.findById(id).ifPresent(story -> {
                story.setDisplayOrder(displayOrder);
                storyRepository.save(story);
            });
        }
    }

    private void copyStoryFields(SuccessStory src, SuccessStory dest) {
        if (src.getTitle() != null) dest.setTitle(src.getTitle());
        if (src.getDescription() != null) dest.setDescription(src.getDescription());
        if (src.getImageUrl() != null) dest.setImageUrl(src.getImageUrl());
        if (src.getCategory() != null) dest.setCategory(src.getCategory());
        dest.setPublished(src.isPublished());
        dest.setFeatured(src.isFeatured());
        dest.setDisplayOrder(src.getDisplayOrder());

        // New enterprise fields
        if (src.getLocation() != null) dest.setLocation(src.getLocation());
        if (src.getSubtitle() != null) dest.setSubtitle(src.getSubtitle());
        if (src.getBeforeImageUrl() != null) dest.setBeforeImageUrl(src.getBeforeImageUrl());
        if (src.getAfterImageUrl() != null) dest.setAfterImageUrl(src.getAfterImageUrl());
        if (src.getVideoUrl() != null) dest.setVideoUrl(src.getVideoUrl());
        if (src.getTestimonialQuote() != null) dest.setTestimonialQuote(src.getTestimonialQuote());
        if (src.getTestimonialAuthor() != null) dest.setTestimonialAuthor(src.getTestimonialAuthor());
    }

    public SuccessStoryResponse mapToResponse(SuccessStory s) {
        // Retrieve gallery media assets
        List<MediaAsset> galleryAssets = mediaAssetRepository.findByOwnerTypeAndOwnerIdOrderByOrderIndexAscIdAsc("STORY", s.getId());

        return SuccessStoryResponse.builder()
                .id(s.getId())
                .title(s.getTitle())
                .description(s.getDescription())
                .imageUrl(s.getImageUrl())
                .category(s.getCategory())
                .published(s.isPublished())
                .featured(s.isFeatured())
                .displayOrder(s.getDisplayOrder())
                .location(s.getLocation())
                .subtitle(s.getSubtitle())
                .beforeImageUrl(s.getBeforeImageUrl())
                .afterImageUrl(s.getAfterImageUrl())
                .videoUrl(s.getVideoUrl())
                .testimonialQuote(s.getTestimonialQuote())
                .testimonialAuthor(s.getTestimonialAuthor())
                .timeline(s.getTimeline() == null ? new ArrayList<>() : s.getTimeline().stream()
                        .map(m -> SuccessStoryResponse.MilestoneResponse.builder()
                                .id(m.getId())
                                .date(m.getDate())
                                .title(m.getTitle())
                                .description(m.getDescription())
                                .imageUrl(m.getImageUrl())
                                .orderIndex(m.getOrderIndex())
                                .build())
                        .collect(Collectors.toList()))
                .metrics(s.getMetrics() == null ? new ArrayList<>() : s.getMetrics().stream()
                        .map(m -> SuccessStoryResponse.MetricResponse.builder()
                                .id(m.getId())
                                .label(m.getLabel())
                                .value(m.getValue())
                                .icon(m.getIcon())
                                .displayOrder(m.getDisplayOrder())
                                .build())
                        .collect(Collectors.toList()))
                .gallery(galleryAssets.stream()
                        .map(m -> SuccessStoryResponse.MediaAssetResponse.builder()
                                .id(m.getId())
                                .mediaType(m.getMediaType().name())
                                .url(m.getUrl())
                                .thumbnailUrl(m.getThumbnailUrl())
                                .caption(m.getCaption())
                                .orderIndex(m.getOrderIndex())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private SuccessStorySummaryResponse mapToSummaryResponse(SuccessStory story) {
        return SuccessStorySummaryResponse.builder()
                .id(story.getId())
                .title(story.getTitle())
                .category(story.getCategory())
                .published(story.isPublished())
                .featured(story.isFeatured())
                .imageUrl(story.getImageUrl())
                .location(story.getLocation())
                .subtitle(story.getSubtitle())
                .displayOrder(story.getDisplayOrder())
                .build();
    }
}
