package com.trustplatform.payment;

import com.razorpay.Payment;
import com.trustplatform.donation.Donation;
import com.trustplatform.donation.DonationRepository;
import com.trustplatform.donation.DonationService;
import com.trustplatform.donation.DonationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Safety net for donations stuck on PROCESSING.
 *
 * A donation is PROCESSING from the moment checkout opens. It normally becomes SUCCESS through the
 * browser callback (/payments/verify) or the Razorpay webhook. If both are lost (tab closed after a UPI
 * app switch, network drop, webhook misconfiguration) the money is taken but the record never updates.
 * This job asks Razorpay directly for the real outcome of every such order.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private static final int MIN_AGE_MINUTES = 5;      // give the normal flows time to finish first
    private static final int ABANDON_AFTER_HOURS = 24; // no payment after this long = checkout abandoned

    private final DonationRepository donationRepository;
    private final DonationService donationService;
    private final RazorpayClientService razorpayClientService;

    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void reconcileStuckDonations() {
        List<Donation> stuck = donationRepository.findByStatusAndGatewayOrderIdIsNotNullAndCreatedAtBefore(
                DonationStatus.PROCESSING, LocalDateTime.now().minusMinutes(MIN_AGE_MINUTES));

        for (Donation d : stuck) {
            String orderId = d.getGatewayOrderId();
            if (orderId == null || orderId.startsWith("order_mock_")) continue;
            try {
                Payment captured = null;
                List<Payment> payments = razorpayClientService.getRazorpayClient().orders.fetchPayments(orderId);
                for (Payment p : payments) {
                    if ("captured".equals(String.valueOf(p.get("status")))) { captured = p; break; }
                }

                if (captured != null) {
                    donationService.processVerifiedDonation(d.getDonorEmail(), d.getAmount(),
                            String.valueOf(captured.get("id")), orderId, "{}");
                    log.warn("[Reconcile] Donation id={} was paid on Razorpay but still PROCESSING: marked SUCCESS", d.getId());
                } else if (d.getCreatedAt() != null
                        && d.getCreatedAt().isBefore(LocalDateTime.now().minusHours(ABANDON_AFTER_HOURS))) {
                    donationService.markAbandoned(d.getId());
                    log.info("[Reconcile] Donation id={} had no payment after {}h: marked FAILED (abandoned)", d.getId(), ABANDON_AFTER_HOURS);
                }
            } catch (Exception e) {
                log.error("[Reconcile] Could not reconcile donation id={} order={}: {}", d.getId(), orderId, e.getMessage());
            }
        }
    }
}
