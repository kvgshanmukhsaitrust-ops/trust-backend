package com.trustplatform.payment.webhook;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "processed_webhook_events")
public class ProcessedWebhookEvent {
    
    @Id
    private String eventId;
    
    @Column(nullable = false)
    private LocalDateTime processedAt;
    
    public ProcessedWebhookEvent() {}
    
    public ProcessedWebhookEvent(String eventId) {
        this.eventId = eventId;
        this.processedAt = LocalDateTime.now();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
