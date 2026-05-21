package com.trustplatform.event;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "event_faqs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EventFaq {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 1000)
    private String question;

    @Column(nullable = false, length = 4000)
    private String answer;

    @Column(name = "display_order")
    private int displayOrder = 0;
}
