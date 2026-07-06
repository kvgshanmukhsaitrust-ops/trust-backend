package com.trustplatform.volunteer;
 
import com.trustplatform.event.Event;
import com.trustplatform.event.EventRepository;
import com.trustplatform.user.User;
import com.trustplatform.user.UserRepository;
import com.trustplatform.volunteer.dto.ApplyVolunteerRequest;
import com.trustplatform.volunteer.dto.VolunteerResponse;
import com.trustplatform.exception.ResourceNotFoundException;
import com.trustplatform.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.trustplatform.notification.NotificationService;
 
import java.util.List;
import java.util.stream.Collectors;
 
@Slf4j
@Service
@RequiredArgsConstructor
public class VolunteerService {
 
    private final VolunteerRepository volunteerRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final NotificationService notificationService;
 
    // ============================
    // APPLY FOR EVENT
    // ============================
    @Transactional
    public VolunteerResponse applyForEvent(ApplyVolunteerRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
 
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
 
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (event.getStatus() == com.trustplatform.event.EventStatus.COMPLETED || 
            (event.getEventDate() != null && event.getEventDate().toLocalDate().isBefore(java.time.LocalDate.now()))) {
            throw new BadRequestException("Cannot apply for a completed event");
        }
 
        if (volunteerRepository.existsByUserIdAndEventId(user.getId(), event.getId())) {
            throw new BadRequestException("You have already applied for this event");
        }
 
        VolunteerApplication application = VolunteerApplication.builder()
                .user(user)
                .event(event)
                .message(request.getMessage())
                .status(VolunteerStatus.PENDING)
                .build();
 
        volunteerRepository.save(application);
        log.info("Volunteer application submitted: userId={}, eventId={}",
                user.getId(), event.getId());

        // Operational Notification: notify admins of the new application
        notificationService.sendToAdmins("New Volunteer Application", 
                "New volunteer application submitted by " + user.getFullName() + " for event " + event.getTitle(), "APPROVAL");
 
        return mapToResponse(application);
    }
 
    // ============================
    // APPROVE VOLUNTEER
    // ============================
    @Transactional
    public void approveVolunteer(Long id) {
        VolunteerApplication application = volunteerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
 
        if (application.getStatus() == VolunteerStatus.APPROVED) {
            throw new BadRequestException("Application is already approved");
        }
        if (application.getStatus() == VolunteerStatus.REJECTED) {
            throw new BadRequestException("Cannot approve a rejected application");
        }
 
        application.setStatus(VolunteerStatus.APPROVED);
        volunteerRepository.save(application);
        log.info("Volunteer approved: applicationId={}, user={}",
                id, application.getUser().getEmail());

        // Operational Notification: notify volunteer of approval
        notificationService.sendToUser(application.getUser().getEmail(), "Volunteer Application Approved", 
                "Congratulations! Your application to participate in the event '" + application.getEvent().getTitle() + "' has been approved.", "APPROVAL");
 
        sendStatusEmail(application, "APPROVED");
    }
 
    // ============================
    // REJECT VOLUNTEER
    // ============================
    @Transactional
    public void rejectVolunteer(Long id) {
        VolunteerApplication application = volunteerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
 
        if (application.getStatus() == VolunteerStatus.REJECTED) {
            throw new BadRequestException("Application is already rejected");
        }
        if (application.getStatus() == VolunteerStatus.APPROVED) {
            throw new BadRequestException("Cannot reject an already approved application");
        }
 
        application.setStatus(VolunteerStatus.REJECTED);
        volunteerRepository.save(application);
        log.info("Volunteer rejected: applicationId={}, user={}",
                id, application.getUser().getEmail());

        // Operational Notification: notify volunteer of rejection
        notificationService.sendToUser(application.getUser().getEmail(), "Volunteer Application Update", 
                "Your application to participate in the event '" + application.getEvent().getTitle() + "' was not accepted at this time.", "APPROVAL");
 
        sendStatusEmail(application, "REJECTED");
    }
 
    // ============================
    // GET ALL APPLICATIONS
    // ============================
    @Transactional(readOnly = true)
    public List<VolunteerResponse> getAllApplications() {
        return volunteerRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
 
    // ============================
    // EMAIL NOTIFICATION LOGIC
    // ============================
    private void sendStatusEmail(VolunteerApplication application, String status) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(application.getUser().getEmail());
            message.setSubject("Volunteer Application Update - "
                    + application.getEvent().getTitle());
 
            String content = "APPROVED".equals(status)
                    ? "Congratulations! Your application for "
                            + application.getEvent().getTitle() + " has been approved."
                    : "We regret to inform you that your application for "
                            + application.getEvent().getTitle()
                            + " was not accepted at this time.";
 
            message.setText("Hello " + application.getUser().getFullName()
                    + ",\n\n" + content + "\n\nRegards,\nTrust Team");
 
            mailSender.send(message);
            log.info("Status email sent to {}", application.getUser().getEmail());
 
        } catch (Exception e) {
            log.error("Failed to send status email to {} for application {}: {}",
                    application.getUser().getEmail(),
                    application.getId(),
                    e.getMessage());
        }
    }
 
    @Transactional(readOnly = true)
    public List<VolunteerResponse> getUserApplications(Long userId) {
        return volunteerRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================
    // DYNAMIC OPERATIONS
    // ============================
    @Transactional
    public VolunteerResponse checkIn(Long id) {
        VolunteerApplication app = volunteerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        app.setCheckInTime(java.time.LocalDateTime.now());
        app = volunteerRepository.save(app);
        return mapToResponse(app);
    }

    @Transactional
    public VolunteerResponse checkOut(Long id) {
        VolunteerApplication app = volunteerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (app.getCheckInTime() == null) {
            throw new BadRequestException("Cannot check out without checking in first");
        }
        app.setCheckOutTime(java.time.LocalDateTime.now());
        
        // Calculate difference in hours
        java.time.Duration duration = java.time.Duration.between(app.getCheckInTime(), app.getCheckOutTime());
        double hours = duration.toMinutes() / 60.0;
        // round to two decimal places
        hours = Math.round(hours * 100.0) / 100.0;
        app.setHoursServed(hours);
        app = volunteerRepository.save(app);
        return mapToResponse(app);
    }

    @Transactional
    public VolunteerResponse assignRole(Long id, String role) {
        VolunteerApplication app = volunteerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        app.setAssignedRole(role);
        app = volunteerRepository.save(app);
        return mapToResponse(app);
    }

    @Transactional
    public VolunteerResponse verifyAttendance(Long id, Double hours, Boolean verified) {
        VolunteerApplication app = volunteerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        app.setAttendanceVerified(verified);
        if (hours != null) {
            app.setHoursServed(hours);
        }
        app = volunteerRepository.save(app);
        return mapToResponse(app);
    }

    @Transactional(readOnly = true)
    public com.trustplatform.volunteer.dto.VolunteerStatsResponse getVolunteerStats(Long userId) {
        List<VolunteerApplication> apps = volunteerRepository.findByUserId(userId);
        
        double totalHours = apps.stream()
                .filter(VolunteerApplication::getAttendanceVerified)
                .mapToDouble(VolunteerApplication::getHoursServed)
                .sum();

        long eventsCount = apps.stream()
                .filter(app -> app.getStatus() == VolunteerStatus.APPROVED && app.getAttendanceVerified())
                .count();

        Long rank = getVolunteerRank(userId);
        List<String> badges = calculateBadges(totalHours, eventsCount);
        
        String tier = "Bronze Helper";
        if (totalHours >= 50.0) {
            tier = "Gold Roster Member";
        } else if (totalHours >= 20.0) {
            tier = "Silver Roster Member";
        }

        double impactScore = (totalHours * 15.0) + (eventsCount * 50.0);

        return com.trustplatform.volunteer.dto.VolunteerStatsResponse.builder()
                .totalHoursServed(totalHours)
                .totalEventsAttended(eventsCount)
                .rank(rank)
                .badges(badges)
                .tier(tier)
                .impactScore(impactScore)
                .build();
    }

    @Transactional(readOnly = true)
    public List<com.trustplatform.volunteer.dto.VolunteerLeaderboardEntry> getLeaderboard() {
        List<VolunteerApplication> allVerified = volunteerRepository.findByAttendanceVerifiedTrue();
        
        // Group by User
        java.util.Map<User, List<VolunteerApplication>> grouped = allVerified.stream()
                .collect(Collectors.groupingBy(VolunteerApplication::getUser));

        List<com.trustplatform.volunteer.dto.VolunteerLeaderboardEntry> entries = grouped.entrySet().stream()
                .map(entry -> {
                    User u = entry.getKey();
                    List<VolunteerApplication> userApps = entry.getValue();
                    double totalHours = userApps.stream().mapToDouble(VolunteerApplication::getHoursServed).sum();
                    long totalEvents = userApps.stream().filter(app -> app.getStatus() == VolunteerStatus.APPROVED).count();

                    String avatarUrl = "https://ui-avatars.com/api/?name=" + 
                            java.net.URLEncoder.encode(u.getFullName(), java.nio.charset.StandardCharsets.UTF_8) + 
                            "&background=B07A3F&color=fff";

                    return com.trustplatform.volunteer.dto.VolunteerLeaderboardEntry.builder()
                            .userId(u.getId())
                            .userFullName(u.getFullName())
                            .avatarUrl(avatarUrl)
                            .totalHoursServed(totalHours)
                            .totalEventsAttended(totalEvents)
                            .build();
                })
                .sorted((e1, e2) -> e2.getTotalHoursServed().compareTo(e1.getTotalHoursServed()))
                .collect(Collectors.toList());

        // Assign ranks
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank((long) (i + 1));
        }

        return entries;
    }

    private Long getVolunteerRank(Long userId) {
        List<com.trustplatform.volunteer.dto.VolunteerLeaderboardEntry> leaderboard = getLeaderboard();
        for (com.trustplatform.volunteer.dto.VolunteerLeaderboardEntry entry : leaderboard) {
            if (entry.getUserId().equals(userId)) {
                return entry.getRank();
            }
        }
        return 1L;
    }

    private List<String> calculateBadges(double hours, long events) {
        java.util.List<String> badges = new java.util.ArrayList<>();
        if (events > 0) {
            badges.add("Pioneer Roster");
        }
        if (events >= 1) {
            badges.add("Mission Initiated");
        }
        if (hours >= 20.0) {
            badges.add("Veteran Server");
        }
        if (events >= 3) {
            badges.add("Impact Champion");
        }
        if (badges.isEmpty()) {
            badges.add("Registered Helper");
        }
        return badges;
    }

    // ============================
    // MAPPER
    // ============================
    private VolunteerResponse mapToResponse(VolunteerApplication app) {
        return VolunteerResponse.builder()
                .id(app.getId())
                .eventId(app.getEvent().getId())
                .eventTitle(app.getEvent().getTitle())
                .userId(app.getUser().getId())
                .userFullName(app.getUser().getFullName())
                .status(app.getStatus())
                .message(app.getMessage())
                .assignedRole(app.getAssignedRole())
                .checkInTime(app.getCheckInTime())
                .checkOutTime(app.getCheckOutTime())
                .hoursServed(app.getHoursServed())
                .attendanceVerified(app.getAttendanceVerified())
                .build();
    }
}