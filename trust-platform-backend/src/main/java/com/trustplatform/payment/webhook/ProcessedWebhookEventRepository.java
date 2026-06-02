package com.trustplatform.payment.webhook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, String> {
}
