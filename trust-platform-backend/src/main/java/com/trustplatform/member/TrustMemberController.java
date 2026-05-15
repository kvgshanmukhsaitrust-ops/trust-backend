package com.trustplatform.member;

import com.trustplatform.member.dto.TrustMemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class TrustMemberController {

    private final TrustMemberRepository repository;

    // ── PUBLIC: published members ordered by displayOrder ────────
    @GetMapping
    public List<TrustMemberResponse> getAllMembers(
            @RequestParam(required = false) Boolean admin) {
        List<TrustMember> members;
        if (Boolean.TRUE.equals(admin)) {
            members = repository.findAllByOrderByDisplayOrderAsc();
        } else {
            members = repository.findByPublishedTrueOrderByDisplayOrderAsc();
        }
        return members.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrustMemberResponse> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(m -> ResponseEntity.ok(toResponse(m)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── ADMIN: create ────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TrustMemberResponse> addMember(@RequestBody TrustMember member) {
        return ResponseEntity.ok(toResponse(repository.save(member)));
    }

    // ── ADMIN: update ────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TrustMemberResponse> updateMember(
            @PathVariable Long id,
            @RequestBody TrustMember updated) {
        return repository.findById(id).map(m -> {
            if (updated.getName() != null) m.setName(updated.getName());
            if (updated.getRole() != null) m.setRole(updated.getRole());
            if (updated.getTagline() != null) m.setTagline(updated.getTagline());
            if (updated.getBio() != null) m.setBio(updated.getBio());
            if (updated.getImageUrl() != null) m.setImageUrl(updated.getImageUrl());
            if (updated.getTwitterUrl() != null) m.setTwitterUrl(updated.getTwitterUrl());
            if (updated.getLinkedinUrl() != null) m.setLinkedinUrl(updated.getLinkedinUrl());
            m.setDisplayOrder(updated.getDisplayOrder());
            m.setPublished(updated.isPublished());
            m.setFeatured(updated.isFeatured());
            return ResponseEntity.ok(toResponse(repository.save(m)));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── ADMIN: delete ────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeMember(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private TrustMemberResponse toResponse(TrustMember m) {
        return TrustMemberResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .role(m.getRole())
                .tagline(m.getTagline())
                .bio(m.getBio())
                .imageUrl(m.getImageUrl())
                .twitterUrl(m.getTwitterUrl())
                .linkedinUrl(m.getLinkedinUrl())
                .displayOrder(m.getDisplayOrder())
                .published(m.isPublished())
                .featured(m.isFeatured())
                .build();
    }
}