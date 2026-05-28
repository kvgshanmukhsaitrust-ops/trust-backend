package com.trustplatform.config;

import com.trustplatform.user.Role;
import com.trustplatform.user.User;
import com.trustplatform.user.UserRepository;
import com.trustplatform.event.Event;
import com.trustplatform.event.EventStatus;
import com.trustplatform.event.EventRepository;
import com.trustplatform.donation.Donation;
import com.trustplatform.donation.DonationStatus;
import com.trustplatform.donation.DonationRepository;
import com.trustplatform.volunteer.VolunteerApplication;
import com.trustplatform.volunteer.VolunteerStatus;
import com.trustplatform.volunteer.VolunteerRepository;
import com.trustplatform.audit.AuditLog;
import com.trustplatform.audit.AuditLogRepository;
import com.trustplatform.notification.Notification;
import com.trustplatform.notification.NotificationRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventRepository eventRepository;
    private final DonationRepository donationRepository;
    private final VolunteerRepository volunteerRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           EventRepository eventRepository, DonationRepository donationRepository,
                           VolunteerRepository volunteerRepository, AuditLogRepository auditLogRepository,
                           NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventRepository = eventRepository;
        this.donationRepository = donationRepository;
        this.volunteerRepository = volunteerRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("[DataInitializer] Starting enterprise platform seeding execution...");

        // 1. Seed Platform Administrator
        String adminEmail = "admin@trust.org";
        User adminUser = null;
        if (!userRepository.existsByEmail(adminEmail)) {
            logger.info("Seeding Admin user account.");
            adminUser = User.builder()
                    .fullName("Platform Administrator")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .isActive(true)
                    .build();
            adminUser = userRepository.save(adminUser);
        } else {
            adminUser = userRepository.findByEmail(adminEmail).orElse(null);
            if (adminUser != null) {
                adminUser.setPassword(passwordEncoder.encode("admin123"));
                adminUser.setRole(Role.ADMIN);
                adminUser.setActive(true);
                adminUser = userRepository.save(adminUser);
            }
            logger.info("Admin user already exists. Force updated credentials.");
        }

        // 2. Seed Mock Donor User
        String donorEmail = "donor@trust.org";
        User donorUser = null;
        if (!userRepository.existsByEmail(donorEmail)) {
            logger.info("Seeding Donor user account.");
            donorUser = User.builder()
                    .fullName("Ananya Deshmukh")
                    .email(donorEmail)
                    .password(passwordEncoder.encode("donor123"))
                    .role(Role.USER)
                    .isActive(true)
                    .build();
            donorUser = userRepository.save(donorUser);
        } else {
            donorUser = userRepository.findByEmail(donorEmail).orElse(null);
            if (donorUser != null) {
                donorUser.setPassword(passwordEncoder.encode("donor123"));
                donorUser.setRole(Role.USER);
                donorUser.setActive(true);
                donorUser = userRepository.save(donorUser);
            }
            logger.info("Donor user already exists. Force updated credentials.");
        }

        // 3. Seed Mock Volunteer User
        String volunteerEmail = "volunteer@trust.org";
        User volunteerUser = null;
        if (!userRepository.existsByEmail(volunteerEmail)) {
            logger.info("Seeding Volunteer user account.");
            volunteerUser = User.builder()
                    .fullName("Kabir Malhotra")
                    .email(volunteerEmail)
                    .password(passwordEncoder.encode("volunteer123"))
                    .role(Role.VOLUNTEER)
                    .isActive(true)
                    .build();
            volunteerUser = userRepository.save(volunteerUser);
        } else {
            volunteerUser = userRepository.findByEmail(volunteerEmail).orElse(null);
            if (volunteerUser != null) {
                volunteerUser.setPassword(passwordEncoder.encode("volunteer123"));
                volunteerUser.setRole(Role.VOLUNTEER);
                volunteerUser.setActive(true);
                volunteerUser = userRepository.save(volunteerUser);
            }
            logger.info("Volunteer user already exists. Force updated credentials.");
        }

        // 4. Seed Mock Normal User
        String normalEmail = "user@trust.org";
        User normalUser = null;
        if (!userRepository.existsByEmail(normalEmail)) {
            logger.info("Seeding Normal user account.");
            normalUser = User.builder()
                    .fullName("Rohit Sharma")
                    .email(normalEmail)
                    .password(passwordEncoder.encode("user123"))
                    .role(Role.USER)
                    .isActive(true)
                    .build();
            normalUser = userRepository.save(normalUser);
        } else {
            normalUser = userRepository.findByEmail(normalEmail).orElse(null);
            if (normalUser != null) {
                normalUser.setPassword(passwordEncoder.encode("user123"));
                normalUser.setRole(Role.USER);
                normalUser.setActive(true);
                normalUser = userRepository.save(normalUser);
            }
            logger.info("Normal user already exists. Force updated credentials.");
        }

        // 5. Seed Core Operational Events
        Event event1 = null;
        Event event2 = null;
        if (eventRepository.count() == 0) {
            logger.info("Seeding default community events.");
            event1 = new Event();
            event1.setTitle("Tuition & Study Center Project");
            event1.setDescription("Providing primary educational coaching classes and evening guidance counseling sessions for underprivileged school kids.");
            event1.setLocation("Sector 5 Study Hub, Nagpur");
            event1.setCategory("EDUCATION");
            event1.setEventDate(LocalDateTime.now().plusDays(10));
            event1.setRegistrationDeadline(LocalDateTime.now().plusDays(8));
            event1.setMaxVolunteers(25);
            event1.setStatus(EventStatus.UPCOMING);
            event1.setPublished(true);
            event1.setFeatured(true);
            event1.setCreatedBy(adminUser);
            event1 = eventRepository.save(event1);

            event2 = new Event();
            event2.setTitle("Pure Water Filtration Outreach");
            event2.setDescription("Distributing reverse-osmosis filtration kits and providing clean sanitization workshops in rural Nagpur suburbs.");
            event2.setLocation("Umred Block Suburbs, Nagpur");
            event2.setCategory("WATER");
            event2.setEventDate(LocalDateTime.now().plusDays(20));
            event2.setRegistrationDeadline(LocalDateTime.now().plusDays(18));
            event2.setMaxVolunteers(15);
            event2.setStatus(EventStatus.UPCOMING);
            event2.setPublished(true);
            event2.setFeatured(true);
            event2.setCreatedBy(adminUser);
            event2 = eventRepository.save(event2);
        } else {
            List<Event> allEvents = eventRepository.findAll();
            if (!allEvents.isEmpty()) event1 = allEvents.get(0);
            if (allEvents.size() > 1) event2 = allEvents.get(1);
        }

        // 6. Seed Realistic Donor History Transactions
        if (donationRepository.count() == 0 && donorUser != null) {
            logger.info("Seeding realistic sample donations.");
            Donation don1 = new Donation();
            don1.setAmount(new BigDecimal("15000.00"));
            don1.setDonorName(donorUser.getFullName());
            don1.setDonorEmail(donorUser.getEmail());
            don1.setMessage("Supporting rural clean drinking water filters.");
            don1.setStatus(DonationStatus.SUCCESS);
            don1.setReceiptNumber("REC-2026-0001");
            don1.setGatewayOrderId("order_ROK853241");
            don1.setTransactionId("pay_ROK853241_TXN");
            don1.setPaymentMethod("UPI");
            don1.setUser(donorUser);
            don1.setEvent(event2);
            donationRepository.save(don1);

            Donation don2 = new Donation();
            don2.setAmount(new BigDecimal("5000.00"));
            don2.setDonorName(donorUser.getFullName());
            don2.setDonorEmail(donorUser.getEmail());
            don2.setMessage("Empowering Nagpur's smart study hubs.");
            don2.setStatus(DonationStatus.SUCCESS);
            don2.setReceiptNumber("REC-2026-0002");
            don2.setGatewayOrderId("order_STU942157");
            don2.setTransactionId("pay_STU942157_TXN");
            don2.setPaymentMethod("Net Banking");
            don2.setUser(donorUser);
            don2.setEvent(event1);
            donationRepository.save(don2);
        }

        // 7. Seed Volunteer applications
        if (volunteerRepository.count() == 0 && volunteerUser != null && event1 != null) {
            logger.info("Seeding volunteer initiative applications.");
            VolunteerApplication app = VolunteerApplication.builder()
                    .user(volunteerUser)
                    .event(event1)
                    .status(VolunteerStatus.PENDING)
                    .message("Keen to support study coaching! I have 2 years of local tutoring experience.")
                    .build();
            volunteerRepository.save(app);
        }

        // 8. Seed Security Audit ledgerTimeline logs
        if (auditLogRepository.count() == 0) {
            logger.info("Seeding system security operations ledger.");
            auditLogRepository.save(AuditLog.builder()
                    .action("AUTH_LOGIN")
                    .performedBy("admin@trust.org")
                    .targetResource("AuthSession")
                    .details("Admin user authenticated successfully from console client.")
                    .ipAddress("192.168.1.100")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now().minusHours(4))
                    .build());

            auditLogRepository.save(AuditLog.builder()
                    .action("UPDATE_CMS_PAGES")
                    .performedBy("admin@trust.org")
                    .targetResource("PagesPanel")
                    .details("Modified Home Page Hero title & History Milestones list safely.")
                    .ipAddress("192.168.1.100")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now().minusHours(2))
                    .build());

            auditLogRepository.save(AuditLog.builder()
                    .action("SECURITY_VIOLATION")
                    .performedBy("guest_attacker@malicious.com")
                    .targetResource("AdminDashboard")
                    .details("Intrusion Alert: Blocked unauthorized API query attempt on root ledger details.")
                    .ipAddress("203.0.113.50")
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Safari/605.1")
                    .status("FAILED")
                    .errorMessage("AccessDeniedException: Unauthorized resource access attempt.")
                    .timestamp(LocalDateTime.now().minusMinutes(45))
                    .build());
        }

        // 9. Seed realtime UI notifications
        if (notificationRepository.count() == 0) {
            logger.info("Seeding admin system alerts.");
            notificationRepository.save(Notification.builder()
                    .recipientEmail("admin@trust.org")
                    .title("Premium Donation Received")
                    .message("Ananya Deshmukh donated ₹15,000 supporting Pure Water Filtration Outreach.")
                    .category("DONATION")
                    .isRead(false)
                    .createdAt(LocalDateTime.now().minusHours(1))
                    .build());

            notificationRepository.save(Notification.builder()
                    .recipientEmail("admin@trust.org")
                    .title("New Volunteer Application")
                    .message("Kabir Malhotra applied for event Tuition & Study Center Project.")
                    .category("APPROVAL")
                    .isRead(false)
                    .createdAt(LocalDateTime.now().minusMinutes(30))
                    .build());
        }

        logger.info("[DataInitializer] Enterprise platform seeding execution successfully completed.");
    }
}
