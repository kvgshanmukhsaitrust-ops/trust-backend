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
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Donation donation;
}