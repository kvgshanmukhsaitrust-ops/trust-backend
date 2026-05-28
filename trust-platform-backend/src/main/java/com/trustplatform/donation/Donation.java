package com.trustplatform.donation;

import com.trustplatform.common.BaseAuditableEntity;
import com.trustplatform.event.Event;
import com.trustplatform.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "donations")
public class Donation extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔹 Donation Amount
    @Column(nullable = false)
    private BigDecimal amount;

    // 🔹 Donor details (for receipts)
    @Column(nullable = false)
    private String donorName;

    @Column(nullable = false)
    private String donorEmail;

    @Column(length = 1000)
    private String message;

    // 🔹 Donation Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DonationStatus status = DonationStatus.PENDING;

    // 🔹 Receipt number
    @Column(unique = true)
    private String receiptNumber;

    // 🔹 Razorpay order ID
    @Column(unique = true)
    private String gatewayOrderId;

    // 🔹 Razorpay payment ID
    @Column(unique = true)
    private String transactionId;

    // 🔹 Raw gateway response
    @Column(length = 2000)
    private String gatewayResponse;

    // 🔹 Payment method (card / upi / netbanking)
    private String paymentMethod;

    // 🔹 Payment error details (if failed)
    @Column(length = 2000)
    private String errorDetails;

    // 🔹 PAN Card (India 80G requirement)
    @Column(length = 50)
    private String donorPan;

    // 🔹 Physical Address (India 80G requirement)
    @Column(length = 1000)
    private String donorAddress;

    // 🔹 Deterministic Receipt UUID for secure archival
    @Column(unique = true, length = 100)
    private String receiptUuid;

    // 🔹 Archived PDF Path
    private String receiptPdfPath;

    // 🔹 Trace Correlation ID propagating across ecosystems
    @Column(length = 100)
    private String correlationId;

    // 🔹 Refund details
    @Column(length = 1000)
    private String refundReason;

    private java.time.LocalDateTime refundDate;

    // =========================
    // USER RELATIONSHIP
    // =========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // =========================
    // EVENT RELATIONSHIP
    // =========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;
}