package com.trustplatform.volunteer;

import com.trustplatform.common.BaseAuditableEntity;
import com.trustplatform.event.Event;
import com.trustplatform.user.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "volunteer_applications",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "event_id"})
        }
)
public class VolunteerApplication extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VolunteerStatus status = VolunteerStatus.PENDING;

    @Column(length = 1000)
    private String message; // optional message from volunteer

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "assigned_role", length = 255)
    private String assignedRole;

    @Column(name = "check_in_time")
    private java.time.LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private java.time.LocalDateTime checkOutTime;

    @Column(name = "hours_served")
    @Builder.Default
    private Double hoursServed = 0.0;

    @Column(name = "attendance_verified", nullable = false)
    @Builder.Default
    private Boolean attendanceVerified = false;
}