package com.trustplatform.impact;

import com.trustplatform.impact.dto.ImpactStatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/impact-stats")
@RequiredArgsConstructor
public class ImpactStatController {

    private final ImpactStatService impactStatService;

    // ── PUBLIC: all stats ordered ─────────────────────────────────
    @GetMapping
    public ResponseEntity<List<ImpactStatResponse>> getAllStats() {
        return ResponseEntity.ok(impactStatService.getAllStats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImpactStatResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(impactStatService.getById(id));
    }

    // ── ADMIN: create ─────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImpactStatResponse> create(@RequestBody ImpactStat stat) {
        return ResponseEntity.ok(impactStatService.create(stat));
    }

    // ── ADMIN: update counter value only (existing API) ───────────
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImpactStatResponse> updateValue(
            @PathVariable Long id,
            @RequestBody Long newValue) {
        return ResponseEntity.ok(impactStatService.update(id, newValue));
    }

    // ── ADMIN: update full stat (icon, unit, category, order) ─────
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImpactStatResponse> updateFull(
            @PathVariable Long id,
            @RequestBody ImpactStat patch) {
        return ResponseEntity.ok(impactStatService.updateFull(id, patch));
    }

    // ── ADMIN: delete ─────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        impactStatService.delete(id);
        return ResponseEntity.noContent().build();
    }
}