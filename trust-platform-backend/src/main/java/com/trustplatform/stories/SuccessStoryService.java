package com.trustplatform.stories;

import com.trustplatform.stories.dto.SuccessStoryResponse;
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

@Service
@RequiredArgsConstructor
public class SuccessStoryService {

    private final SuccessStoryRepository storyRepository;
    private final StoryTimelineMilestoneRepository milestoneRepository;
    private final StoryImpactMetricRepository metricRepository;
    private final MediaAssetRepository mediaAssetRepository;

    @Transactional(readOnly = true)
    public List<SuccessStoryResponse> getAllStories(Boolean admin) {
        List<SuccessStory> stories;
        if (Boolean.TRUE.equals(admin)) {
            stories = storyRepository.findAll();
        } else {
            stories = storyRepository.findByPublishedTrueOrderByDisplayOrderAscIdDesc();
        }
        return stories.stream().map(this::mapToResponse).collect(Collectors.toList());
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
}
