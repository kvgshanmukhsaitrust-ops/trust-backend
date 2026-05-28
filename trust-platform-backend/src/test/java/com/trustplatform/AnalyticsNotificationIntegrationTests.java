package com.trustplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustplatform.analytics.AnalyticsService;
import com.trustplatform.donation.Donation;
import com.trustplatform.donation.DonationRepository;
import com.trustplatform.donation.DonationStatus;
import com.trustplatform.notification.Notification;
import com.trustplatform.notification.NotificationRepository;
import com.trustplatform.notification.NotificationService;
import com.trustplatform.payment.transaction.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AnalyticsNotificationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        // Clean up repositories manually to avoid @Transactional isolation mismatch with @Async tasks
        notificationRepository.deleteAll();
        paymentTransactionRepository.deleteAll();
        donationRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "analyst@trust.org", authorities = {"VIEW_ANALYTICS"})
    public void whenAuthorizedUserFetchesAnalytics_thenReturns200AndCorrectAggregations() throws Exception {
        // Seed some donations to verify stream aggregation works
        Donation d1 = new Donation();
        d1.setDonorName("John Doe");
        d1.setDonorEmail("john@example.com");
        d1.setAmount(new BigDecimal("150.00"));
        d1.setStatus(DonationStatus.SUCCESS);
        d1.setPaymentMethod("RAZORPAY");

        Donation d2 = new Donation();
        d2.setDonorName("Jane Smith");
        d2.setDonorEmail("jane@example.com");
        d2.setAmount(new BigDecimal("350.00"));
        d2.setStatus(DonationStatus.SUCCESS);
        d2.setPaymentMethod("STRIPE");

        Donation d3 = new Donation();
        d3.setDonorName("John Doe");
        d3.setDonorEmail("john@example.com"); // Repeat donor
        d3.setAmount(new BigDecimal("100.00"));
        d3.setStatus(DonationStatus.SUCCESS);
        d3.setPaymentMethod("RAZORPAY");

        donationRepository.saveAll(List.of(d1, d2, d3));

        mockMvc.perform(get("/api/admin/analytics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.donorAnalytics.totalDonorsCount").value(2))
                .andExpect(jsonPath("$.data.donorAnalytics.repeatDonorsCount").value(1))
                .andExpect(jsonPath("$.data.donorAnalytics.averageDonationSize").value(200.00));
    }

    @Test
    @WithMockUser(username = "unauthorized@trust.org", authorities = {"READ_CONTENT"})
    public void whenUnauthorizedUserAttemptsAnalyticsFetch_thenReturns403AndBroadcastsSecurityViolation() throws Exception {
        // Attempting to fetch protected analytics without VIEW_ANALYTICS authority
        mockMvc.perform(get("/api/admin/analytics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Wait brief moment for async notification push in background thread
        Thread.sleep(200);

        // Verify that a broadcast notification was saved under ROLE_ADMIN
        List<Notification> alerts = notificationRepository.findAll();
        assertThat(alerts).isNotEmpty();

        Notification violationAlert = alerts.stream()
                .filter(a -> "ROLE_ADMIN".equals(a.getRecipientEmail()))
                .filter(a -> "SYSTEM".equals(a.getCategory()))
                .findFirst()
                .orElse(null);

        assertThat(violationAlert).isNotNull();
        assertThat(violationAlert.getTitle()).isEqualTo("Security Violation Warning");
        assertThat(violationAlert.getMessage()).contains("/api/admin/analytics");
        assertThat(violationAlert.getMessage()).contains("unauthorized@trust.org");
    }

    @Test
    @WithMockUser(username = "admin@trust.org", authorities = {"READ_CONTENT"})
    public void whenNotificationsDispatched_thenRetrievesCorrectlyAndCanMarkAsRead() throws Exception {
        // Manually trigger private notifications
        notificationService.sendToAdmins("Task Assigned", "Review the green planting campaign", "WORKFLOW");
        notificationService.sendToAdmins("New Donation Received", "₹5,000 received via Razorpay", "DONATION");

        // Wait brief moment for async persistence
        Thread.sleep(200);

        // Verify GET /api/notifications returns the items
        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isNotEmpty());

        // Verify unread count
        mockMvc.perform(get("/api/notifications/unread-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));

        // Get notification id to mark as read
        List<Notification> allAlerts = notificationRepository.findAll();
        Long alertId = allAlerts.get(0).getId();

        // Mark single as read
        mockMvc.perform(put("/api/notifications/" + alertId + "/read")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Check count is now 1
        mockMvc.perform(get("/api/notifications/unread-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        // Mark all as read
        mockMvc.perform(post("/api/notifications/read-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Check count is now 0
        mockMvc.perform(get("/api/notifications/unread-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }
}
