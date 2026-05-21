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
                .build();
    }
}