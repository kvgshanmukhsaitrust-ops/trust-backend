package com.trustplatform.donation;

import com.trustplatform.donation.dto.PublicDonorEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * Public, read-only donation feed that powers the homepage "live donors" leaderboard.
 * Only SUCCESS donations are returned and donor names are reduced to a first name
 * plus last initial.
 */
@RestController
@RequestMapping("/api/public/donations")
@RequiredArgsConstructor
public class PublicDonationController {

    private final DonationRepository donationRepository;

    @GetMapping("/leaderboard")
    public ResponseEntity<List<PublicDonorEntry>> leaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        int size = Math.max(1, Math.min(limit, 20));
        List<PublicDonorEntry> entries = donationRepository
                .findByStatusOrderByCreatedAtDesc(DonationStatus.SUCCESS, PageRequest.of(0, size))
                .stream()
                .map(d -> new PublicDonorEntry(maskName(d.getDonorName()), d.getAmount(), d.getCreatedAt()))
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(10)).cachePublic())
                .body(entries);
    }

    static String maskName(String full) {
        if (full == null || full.isBlank()) return "Kind Donor";
        String[] parts = full.trim().split("\s+");
        String first = parts[0];
        if (first.length() > 20) first = first.substring(0, 20);
        if (parts.length > 1 && !parts[parts.length - 1].isEmpty()) {
            return first + " " + Character.toUpperCase(parts[parts.length - 1].charAt(0)) + ".";
        }
        return first;
    }
}
