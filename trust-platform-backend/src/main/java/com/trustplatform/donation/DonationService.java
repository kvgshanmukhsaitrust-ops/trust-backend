package com.trustplatform.donation;

import com.trustplatform.donation.dto.CreateDonationRequest;
import com.trustplatform.donation.dto.DonationResponse;
import com.trustplatform.event.Event;
import com.trustplatform.event.EventRepository;
import com.trustplatform.exception.ResourceNotFoundException;
import com.trustplatform.user.User;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DonationService {

    private final DonationRepository donationRepository;
    private final EventRepository eventRepository;

    // =========================================
    // CREATE DONATION
    // =========================================

    public DonationResponse createDonation(@Valid CreateDonationRequest request) {

        Donation donation = new Donation();
        donation.setAmount(request.getAmount());
        donation.setDonorName(request.getDonorName());
        donation.setDonorEmail(request.getDonorEmail());
        donation.setMessage(request.getMessage());
        donation.setStatus(DonationStatus.PENDING);

        // Optional event linking
        if (request.getEventId() != null) {
            Event event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
            donation.setEvent(event);
        }

        donation = donationRepository.save(donation);

        return mapToResponse(donation);
    }

    // =========================================
    // GET DONATIONS WITH OPTIONAL STATUS FILTER
    // =========================================

    public Page<DonationResponse> getDonations(DonationStatus status, Pageable pageable) {

        Page<Donation> donationPage;

        if (status != null) {
            donationPage = donationRepository.findByStatus(status, pageable);
        } else {
            donationPage = donationRepository.findAll(pageable);
        }

        return donationPage.map(this::mapToResponse);
    }

    // =========================================
    // MARK DONATION SUCCESS
    // =========================================

    public void markDonationSuccess(Long id, String transactionId) {

        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

        donation.setStatus(DonationStatus.SUCCESS);
        donation.setTransactionId(transactionId);
        donation.setReceiptNumber(generateReceiptNumber());

        donationRepository.save(donation);
    }

    // =========================================
    // MARK DONATION FAILED
    // =========================================

    public void markDonationFailed(Long id) {

        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

        donation.setStatus(DonationStatus.FAILED);
        donationRepository.save(donation);
    }

    // =========================================
    // PRIVATE METHODS
    // =========================================

    private String generateReceiptNumber() {
        return "REC-" + UUID.randomUUID().toString()
                .substring(0, 8)
                .toUpperCase();
    }

    private DonationResponse mapToResponse(Donation donation) {
        return DonationResponse.builder()
                .id(donation.getId())
                .amount(donation.getAmount())
                .donorName(donation.getDonorName())
                .donorEmail(donation.getDonorEmail())
                .message(donation.getMessage())
                .status(donation.getStatus())
                .receiptNumber(donation.getReceiptNumber())
                .createdAt(donation.getCreatedAt())
                .build();
    }
    public List<Donation> getUserDonations(Long userId) {

        return donationRepository.findByUser_Id(userId);
    }

    public Donation getDonationById(Long donationId) {

        return donationRepository.findById(donationId)
                .orElseThrow(() -> new RuntimeException("Donation not found"));
    }
    public Donation createDonation(User user, CreateDonationRequest request) {

        Donation donation = new Donation();

        donation.setAmount(request.getAmount());
        donation.setDonorName(request.getDonorName());
        donation.setDonorEmail(request.getDonorEmail());
        donation.setMessage(request.getMessage());

        donation.setUser(user);

        return donationRepository.save(donation);
    }
}