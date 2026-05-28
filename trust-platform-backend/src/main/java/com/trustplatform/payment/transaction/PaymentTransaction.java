package com.trustplatform.payment.transaction;

import com.trustplatform.common.BaseAuditableEntity;
import com.trustplatform.donation.Donation;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String gateway;

    private String gatewayOrderId;

    private String gatewayPaymentId;

    private String gatewaySignature;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    @Column(nullable = false, unique = true, length = 100)
    private String correlationId; // strictly unique correlation ID propagating across all flows

    private java.math.BigDecimal amount;

    private String paymentMethod;

    @Column(length = 2000)
    private String errorDetails;

    @Column(length = 2000)
    private String metadata; // additional reconciliation json

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id", nullable = false)
    private Donation donation;
}