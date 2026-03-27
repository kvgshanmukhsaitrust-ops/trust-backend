package com.trustplatform.donation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findByUser_Id(Long userId);

    Page<Donation> findByStatus(DonationStatus status, Pageable pageable);

    Optional<Donation> findByGatewayOrderId(String gatewayOrderId);
}