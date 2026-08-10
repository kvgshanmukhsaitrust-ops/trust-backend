package com.trustplatform;

import com.trustplatform.donation.Donation;
import com.trustplatform.donation.DonationRepository;
import com.trustplatform.donation.DonationStatus;
import com.trustplatform.user.User;
import com.trustplatform.user.UserRepository;
import com.trustplatform.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DonationSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private User owner;
    private User otherUser;
    private Donation ownerDonation;
    private Donation guestDonation;

    @BeforeEach
    public void setup() {
        // Create owner user
        owner = new User();
        owner.setEmail("owner@example.com");
        owner.setPassword("password123");
        owner.setFullName("Donation Owner");
        owner.setRole(Role.USER);
        owner = userRepository.save(owner);

        // Create another user
        otherUser = new User();
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("password123");
        otherUser.setFullName("Other User");
        otherUser.setRole(Role.USER);
        otherUser = userRepository.save(otherUser);

        // Create donation for owner
        ownerDonation = new Donation();
        ownerDonation.setAmount(BigDecimal.valueOf(1000));
        ownerDonation.setDonorName(owner.getFullName());
        ownerDonation.setDonorEmail(owner.getEmail());
        ownerDonation.setStatus(DonationStatus.SUCCESS);
        ownerDonation.setUser(owner);
        ownerDonation.setReceiptUuid(UUID.randomUUID().toString());
        ownerDonation = donationRepository.save(ownerDonation);

        // Create guest donation
        guestDonation = new Donation();
        guestDonation.setAmount(BigDecimal.valueOf(500));
        guestDonation.setDonorName("Guest Donor");
        guestDonation.setDonorEmail("guest@example.com");
        guestDonation.setStatus(DonationStatus.SUCCESS);
        guestDonation.setUser(null); // Guest
        guestDonation.setReceiptUuid(UUID.randomUUID().toString());
        guestDonation = donationRepository.save(guestDonation);
    }

    @Test
    public void whenUnauthenticatedUserAttemptsReceiptDownload_thenReturns401() throws Exception {
        // Attempting to download owner's receipt without login
        mockMvc.perform(get("/api/donations/" + ownerDonation.getId() + "/receipt")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "other@example.com", authorities = {"READ_CONTENT"})
    public void whenOtherUserAttemptsAccessToDonationRecord_thenReturns403() throws Exception {
        // Attempting to read another user's donation record
        mockMvc.perform(get("/api/donations/" + ownerDonation.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "other@example.com", authorities = {"READ_CONTENT"})
    public void whenOtherUserAttemptsReceiptDownload_thenReturns403() throws Exception {
        // Attempting to download another user's receipt
        mockMvc.perform(get("/api/donations/" + ownerDonation.getId() + "/receipt")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner@example.com", authorities = {"READ_CONTENT"})
    public void whenOwnerAttemptsAccessToDonationRecord_thenSucceeds() throws Exception {
        // Owner reading their own donation record
        mockMvc.perform(get("/api/donations/" + ownerDonation.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"VIEW_ANALYTICS"})
    public void whenAdminAttemptsAccessToDonationRecord_thenSucceeds() throws Exception {
        // Admin reading user's donation record
        mockMvc.perform(get("/api/donations/" + ownerDonation.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void whenGuestDownloadsReceiptWithValidUuid_thenSucceeds() throws Exception {
        // Guest downloading receipt using valid UUID without credentials
        mockMvc.perform(get("/api/donations/receipt/uuid/" + guestDonation.getReceiptUuid())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void whenGuestDownloadsReceiptWithInvalidUuid_thenReturns404() throws Exception {
        // Guest downloading receipt with invalid UUID
        mockMvc.perform(get("/api/donations/receipt/uuid/invalid-uuid-string")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void whenSavingDonation_thenPanIsEncryptedInDatabaseAndDecryptedOnRead() {
        Donation donation = new Donation();
        donation.setAmount(BigDecimal.valueOf(200));
        donation.setDonorName("Crypt Test");
        donation.setDonorEmail("crypt@example.com");
        donation.setDonorPan("ABCDE1234F");
        donation.setStatus(DonationStatus.PENDING);
        donation = donationRepository.saveAndFlush(donation);

        // Fetch directly via JDBC to bypass JPA decryption
        String rawPan = jdbcTemplate.queryForObject(
                "SELECT donor_pan FROM donations WHERE id = ?",
                String.class,
                donation.getId()
        );

        // Verify it is encrypted (not plain text)
        org.junit.jupiter.api.Assertions.assertNotNull(rawPan);
        org.junit.jupiter.api.Assertions.assertNotEquals("ABCDE1234F", rawPan);
        
        // Verify it is base64 GCM (not human readable plaintext)
        org.junit.jupiter.api.Assertions.assertTrue(rawPan.length() > 20);

        // Fetch via JPA to verify transparent decryption
        Donation retrieved = donationRepository.findById(donation.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("ABCDE1234F", retrieved.getDonorPan());
    }
}
