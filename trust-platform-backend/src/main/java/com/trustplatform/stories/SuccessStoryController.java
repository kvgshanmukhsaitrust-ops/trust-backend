package com.trustplatform.stories;

import com.trustplatform.stories.dto.SuccessStoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/success-stories")
@RequiredArgsConstructor
public class SuccessStoryController {

    private final SuccessStoryRepository repository;

    @GetMapping
    public List<SuccessStoryResponse> getAllStories() {
        return repository.findAll().stream()
                .map(story -> SuccessStoryResponse.builder()
                        .id(story.getId())
                        .title(story.getTitle())
                        .description(story.getDescription())
                        .imageUrl(story.getImageUrl())
                        .category(story.getCategory())
                        .build())
                .collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SuccessStoryResponse createStory(@RequestBody SuccessStory story) {
        SuccessStory saved = repository.save(story);
        return SuccessStoryResponse.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .description(saved.getDescription())
                .imageUrl(saved.getImageUrl())
                .category(saved.getCategory())
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteStory(@PathVariable Long id) {
        repository.deleteById(id);
    }
}