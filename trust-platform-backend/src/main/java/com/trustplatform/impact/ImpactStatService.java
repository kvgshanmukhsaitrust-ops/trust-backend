package com.trustplatform.impact;

import com.trustplatform.impact.dto.ImpactStatResponse;
import com.trustplatform.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImpactStatService {

    private final ImpactStatRepository impactStatRepository;

    @Transactional(readOnly = true)
    public List<ImpactStatResponse> getAllStats() {
        return impactStatRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ImpactStatResponse getById(Long id) {
        return impactStatRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Stat not found: " + id));
    }

    @Transactional
    public ImpactStatResponse update(Long id, Long newValue) {
        ImpactStat stat = impactStatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stat not found: " + id));
        log.info("Impact counter updated: id={}, category={}, old={}, new={}",
                id, stat.getCategory(), stat.getCurrentValue(), newValue);
        stat.setCurrentValue(newValue);
        return toResponse(impactStatRepository.save(stat));
    }

    @Transactional
    public ImpactStatResponse updateFull(Long id, ImpactStat patch) {
        ImpactStat stat = impactStatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stat not found: " + id));
        if (patch.getCategory() != null && !patch.getCategory().isBlank()) {
            stat.setCategory(patch.getCategory());
        }
        if (patch.getCurrentValue() != null) stat.setCurrentValue(patch.getCurrentValue());
        if (patch.getUnit() != null) stat.setUnit(patch.getUnit());
        if (patch.getIcon() != null) stat.setIcon(patch.getIcon());
        stat.setFeatured(patch.isFeatured());
        stat.setDisplayOrder(patch.getDisplayOrder());
        return toResponse(impactStatRepository.save(stat));
    }

    @Transactional
    public ImpactStatResponse create(ImpactStat stat) {
        return toResponse(impactStatRepository.save(stat));
    }

    @Transactional
    public void delete(Long id) {
        if (!impactStatRepository.existsById(id)) {
            throw new ResourceNotFoundException("Stat not found: " + id);
        }
        impactStatRepository.deleteById(id);
    }

    private ImpactStatResponse toResponse(ImpactStat s) {
        return ImpactStatResponse.builder()
                .id(s.getId())
                .category(s.getCategory())
                .currentValue(s.getCurrentValue())
                .unit(s.getUnit())
                .icon(s.getIcon())
                .featured(s.isFeatured())
                .displayOrder(s.getDisplayOrder())
                .build();
    }
}