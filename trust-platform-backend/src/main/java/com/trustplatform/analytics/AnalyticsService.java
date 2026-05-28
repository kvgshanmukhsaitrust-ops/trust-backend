package com.trustplatform.analytics;

import com.trustplatform.analytics.dto.*;
import com.trustplatform.common.ContentVersionRepository;
import com.trustplatform.donation.Donation;
import com.trustplatform.donation.DonationRepository;
import com.trustplatform.donation.DonationStatus;
import com.trustplatform.event.Event;
import com.trustplatform.event.EventRepository;
import com.trustplatform.media.MediaAsset;
import com.trustplatform.media.MediaAssetRepository;
import com.trustplatform.stories.SuccessStory;
import com.trustplatform.stories.SuccessStoryRepository;
import com.trustplatform.volunteer.VolunteerApplication;
import com.trustplatform.volunteer.VolunteerRepository;
import com.trustplatform.volunteer.VolunteerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final DonationRepository donationRepository;
    private final EventRepository eventRepository;
    private final VolunteerRepository volunteerRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final SuccessStoryRepository successStoryRepository;
    private final ContentVersionRepository versionRepository;

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getAnalyticsSummary() {
        log.info("Generating SaaS-grade aggregation stats for Operational Analytics Engine...");

        // 1. Donor Analytics
        List<Donation> allDonations = donationRepository.findAll();
        List<Donation> successDonations = allDonations.stream()
                .filter(d -> d.getStatus() == DonationStatus.SUCCESS)
                .collect(Collectors.toList());

        long totalDonors = successDonations.stream()
                .map(Donation::getDonorEmail)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        Map<String, Long> donorCounts = successDonations.stream()
                .map(Donation::getDonorEmail)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(email -> email, Collectors.counting()));
        long repeatDonors = donorCounts.values().stream()
                .filter(count -> count > 1)
                .count();

        BigDecimal totalSuccessAmount = successDonations.stream()
                .map(d -> d.getAmount() != null ? d.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageDonation = BigDecimal.ZERO;
        if (!successDonations.isEmpty()) {
            averageDonation = totalSuccessAmount.divide(BigDecimal.valueOf(successDonations.size()), 2, RoundingMode.HALF_UP);
        }

        Map<String, Long> paymentMethodBreakdown = successDonations.stream()
                .filter(d -> d.getPaymentMethod() != null)
                .collect(Collectors.groupingBy(Donation::getPaymentMethod, Collectors.counting()));

        DonorStat donorStat = DonorStat.builder()
                .totalDonorsCount(totalDonors)
                .repeatDonorsCount(repeatDonors)
                .averageDonationSize(averageDonation)
                .paymentMethodBreakdown(paymentMethodBreakdown)
                .build();

        // 2. Campaign Analytics
        Map<Event, List<Donation>> donationsByCampaign = successDonations.stream()
                .filter(d -> d.getEvent() != null)
                .collect(Collectors.groupingBy(Donation::getEvent));

        List<CampaignStat> campaignStats = donationsByCampaign.entrySet().stream()
                .map(entry -> {
                    Event e = entry.getKey();
                    BigDecimal raised = entry.getValue().stream()
                            .map(d -> d.getAmount() != null ? d.getAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return CampaignStat.builder()
                            .eventId(e.getId())
                            .eventTitle(e.getTitle())
                            .totalRaised(raised)
                            .donationCount(entry.getValue().size())
                            .build();
                })
                .sorted(Comparator.comparing(CampaignStat::getTotalRaised).reversed())
                .collect(Collectors.toList());

        // 3. Event Engagement
        List<Event> allEvents = eventRepository.findAll();
        List<VolunteerApplication> allVolunteers = volunteerRepository.findAll();

        List<EventEngagementStat> engagementStats = allEvents.stream()
                .map(e -> {
                    List<VolunteerApplication> eventApps = allVolunteers.stream()
                            .filter(v -> v.getEvent() != null && v.getEvent().getId().equals(e.getId()))
                            .collect(Collectors.toList());

                    long totalApps = eventApps.size();
                    long approved = eventApps.stream().filter(v -> v.getStatus() == VolunteerStatus.APPROVED).count();
                    double approvalRate = totalApps > 0 ? (double) approved / totalApps : 0.0;

                    return EventEngagementStat.builder()
                            .eventId(e.getId())
                            .eventTitle(e.getTitle())
                            .status(e.getStatus() != null ? e.getStatus().name() : "UPCOMING")
                            .maxVolunteers(e.getMaxVolunteers() != null ? e.getMaxVolunteers() : 0)
                            .totalApplications(totalApps)
                            .approvalRate(approvalRate)
                            .build();
                })
                .sorted(Comparator.comparing(EventEngagementStat::getTotalApplications).reversed())
                .collect(Collectors.toList());

        // 4. Volunteer Metrics
        long totalVolunteers = allVolunteers.stream()
                .map(v -> v.getUser() != null ? v.getUser().getEmail() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        long totalAppsCount = allVolunteers.size();
        long approvedApps = allVolunteers.stream().filter(v -> v.getStatus() == VolunteerStatus.APPROVED).count();
        long pendingApps = allVolunteers.stream().filter(v -> v.getStatus() == VolunteerStatus.PENDING).count();

        double approvedRatio = totalAppsCount > 0 ? (double) approvedApps / totalAppsCount : 0.0;
        double pendingRatio = totalAppsCount > 0 ? (double) pendingApps / totalAppsCount : 0.0;

        // 5. Media Metrics
        List<MediaAsset> allMedia = mediaAssetRepository.findAll();
        long imageCount = allMedia.stream().filter(m -> m.getMediaType() != null && "IMAGE".equalsIgnoreCase(m.getMediaType().name())).count();
        long videoCount = allMedia.stream().filter(m -> m.getMediaType() != null && "VIDEO".equalsIgnoreCase(m.getMediaType().name())).count();

        long totalContent = allEvents.size() + successStoryRepository.count();
        double avgMedia = totalContent > 0 ? (double) allMedia.size() / totalContent : 0.0;

        // 6. Story Performance Metrics
        List<SuccessStory> allStories = successStoryRepository.findAll();
        long publishedStories = allStories.stream().filter(SuccessStory::isPublished).count();
        long draftStories = allStories.size() - publishedStories;

        // Calculate average versions per story from ContentVersion
        long totalVersions = versionRepository.count();
        double avgVersions = allStories.size() > 0 ? (double) totalVersions / allStories.size() : 1.0;

        return AnalyticsSummaryResponse.builder()
                .donorAnalytics(donorStat)
                .campaignAnalytics(campaignStats)
                .eventEngagement(engagementStats)
                .totalVolunteersCount(totalVolunteers)
                .volunteerApprovedRatio(approvedRatio)
                .volunteerPendingRatio(pendingRatio)
                .imageCount(imageCount)
                .videoCount(videoCount)
                .averageMediaItemsPerContent(avgMedia)
                .publishedStoriesCount(publishedStories)
                .draftStoriesCount(draftStories)
                .averageVersionsPerStory(avgVersions)
                .build();
    }
}
