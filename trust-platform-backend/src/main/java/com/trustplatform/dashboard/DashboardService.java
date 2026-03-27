package com.trustplatform.dashboard;

import com.trustplatform.dashboard.dto.DashboardSummaryResponse;
import com.trustplatform.dashboard.dto.MonthlyDonationStats;
import com.trustplatform.donation.Donation;
import com.trustplatform.donation.DonationRepository;
import com.trustplatform.donation.DonationStatus;
import com.trustplatform.event.EventRepository;
import com.trustplatform.event.EventStatus;
import com.trustplatform.volunteer.VolunteerRepository;
import com.trustplatform.volunteer.VolunteerStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DonationRepository donationRepository;
    private final EventRepository eventRepository;
    private final VolunteerRepository volunteerRepository;

    public DashboardSummaryResponse getDashboardSummary() {

        List<Donation> donations = donationRepository.findAll();

        BigDecimal totalAmount = donations.stream()
                .filter(d -> d.getStatus() == DonationStatus.SUCCESS)
                .map(Donation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long successfulCount = donations.stream()
                .filter(d -> d.getStatus() == DonationStatus.SUCCESS)
                .count();

        Long pendingCount = donations.stream()
                .filter(d -> d.getStatus() == DonationStatus.PENDING)
                .count();

        // Monthly breakdown
        List<MonthlyDonationStats> monthlyStats = new ArrayList<>();

        for (Month month : Month.values()) {
            BigDecimal monthTotal = donations.stream()
                    .filter(d -> d.getStatus() == DonationStatus.SUCCESS)
                    .filter(d -> d.getCreatedAt().getMonth() == month)
                    .map(Donation::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            monthlyStats.add(new MonthlyDonationStats(
                    month.name(),
                    monthTotal
            ));
        }

        return DashboardSummaryResponse.builder()
                .totalAmountCollected(totalAmount)
                .totalDonations((long) donations.size())
                .successfulDonations(successfulCount)
                .pendingDonations(pendingCount)

                .totalEvents(eventRepository.count())
                .upcomingEvents(eventRepository.countByStatus(EventStatus.UPCOMING))
                .completedEvents(eventRepository.countByStatus(EventStatus.COMPLETED))

                .totalApplications(volunteerRepository.count())
                .approvedVolunteers(
                        volunteerRepository.countByStatus(VolunteerStatus.APPROVED)
                )
                .pendingApplications(
                        volunteerRepository.countByStatus(VolunteerStatus.PENDING)
                )

                .monthlyDonations(monthlyStats)
                .build();
    }
}