package com.trustplatform.donation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    @org.springframework.data.jpa.repository.Query("SELECT d FROM Donation d LEFT JOIN FETCH d.user WHERE d.id = :id")
    Optional<Donation> findByIdWithUser(@org.springframework.data.repository.query.Param("id") Long id);

    List<Donation> findByUser_Id(Long userId);

    Page<Donation> findByStatus(DonationStatus status, Pageable pageable);

    Optional<Donation> findByGatewayOrderId(String gatewayOrderId);

    Optional<Donation> findByReceiptUuid(String receiptUuid);
}