package com.trustplatform.impact;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.trustplatform.audit.AuditAction;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ImpactShowcaseController {

    private final ImpactShowcaseCardRepository repository;

    @GetMapping("/api/public/impact-showcase/all")
    public ResponseEntity<List<ImpactShowcaseCard>> getAllShowcases() {
        return ResponseEntity.ok(repository.findAllByDeletedFalseOrderByDisplayOrderAsc());
    }

    @PostMapping("/api/admin/impact-showcase")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditAction("CREATE_SHOWCASE_CARD")
    public ResponseEntity<ImpactShowcaseCard> create(@RequestBody ImpactShowcaseCard card) {
        card.setDescription(com.trustplatform.common.HtmlSanitizer.sanitize(card.getDescription()));
        return ResponseEntity.ok(repository.save(card));
    }

    @PutMapping("/api/admin/impact-showcase/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditAction("UPDATE_SHOWCASE_CARD")
    public ResponseEntity<ImpactShowcaseCard> update(@PathVariable Long id, @RequestBody ImpactShowcaseCard cardDetails) {
        ImpactShowcaseCard card = repository.findById(id)
                .orElseThrow(() -> new com.trustplatform.exception.ResourceNotFoundException("ImpactShowcaseCard not found with id: " + id));
        card.setTitle(cardDetails.getTitle());
        card.setDescription(com.trustplatform.common.HtmlSanitizer.sanitize(cardDetails.getDescription()));
        card.setIcon(cardDetails.getIcon());
        card.setMetricCount(cardDetails.getMetricCount());
        card.setDisplayOrder(cardDetails.getDisplayOrder());
        card.setSubtitle(cardDetails.getSubtitle());
        card.setBaseImage(cardDetails.getBaseImage());
        card.setRevealImage(cardDetails.getRevealImage());
        card.setStatLabel(cardDetails.getStatLabel());
        card.setTags(cardDetails.getTags());
        card.setAccentColor(cardDetails.getAccentColor());
        return ResponseEntity.ok(repository.save(card));
    }

    @DeleteMapping("/api/admin/impact-showcase/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditAction("SOFT_DELETE_SHOWCASE_CARD")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ImpactShowcaseCard card = repository.findById(id)
                .orElseThrow(() -> new com.trustplatform.exception.ResourceNotFoundException("ImpactShowcaseCard not found with id: " + id));
        card.setDeleted(true);
        repository.save(card);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/admin/impact-showcase/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditAction("REORDER_SHOWCASE_CARDS")
    public ResponseEntity<Void> reorderCards(@RequestBody List<Long> cardIds) {
        for (int i = 0; i < cardIds.size(); i++) {
            Long id = cardIds.get(i);
            final int displayOrderIndex = i;
            repository.findById(id).ifPresent(card -> {
                card.setDisplayOrder(displayOrderIndex);
                repository.save(card);
            });
        }
        return ResponseEntity.ok().build();
    }
}
