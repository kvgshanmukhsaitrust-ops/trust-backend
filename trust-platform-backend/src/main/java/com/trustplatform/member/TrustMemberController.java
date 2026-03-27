package com.trustplatform.member;

import com.trustplatform.member.dto.TrustMemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class TrustMemberController {

    private final TrustMemberRepository repository;

    @GetMapping
    public List<TrustMemberResponse> getAllMembers() {
        return repository.findAllByOrderByDisplayOrderAsc().stream()
                .map(member -> TrustMemberResponse.builder()
                        .id(member.getId())
                        .name(member.getName())
                        .role(member.getRole())
                        .tagline(member.getTagline())
                        .bio(member.getBio())
                        .imageUrl(member.getImageUrl())
                        .twitterUrl(member.getTwitterUrl())
                        .linkedinUrl(member.getLinkedinUrl())
                        .build())
                .collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public TrustMemberResponse addMember(@RequestBody TrustMember member) {
        TrustMember saved = repository.save(member);
        return TrustMemberResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .role(saved.getRole())
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void removeMember(@PathVariable Long id) {
        repository.deleteById(id);
    }
}