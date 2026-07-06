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
    private final com.trustplatform.impact.ImpactStatRepository impactStatRepository;
    private final com.trustplatform.member.TrustMemberRepository trustMemberRepository;
    private final com.trustplatform.stories.SuccessStoryRepository successStoryRepository;
    private final com.trustplatform.impact.ImpactShowcaseCardRepository impactShowcaseCardRepository;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           EventRepository eventRepository, DonationRepository donationRepository,
                           VolunteerRepository volunteerRepository, AuditLogRepository auditLogRepository,
                           NotificationRepository notificationRepository,
                           com.trustplatform.impact.ImpactStatRepository impactStatRepository,
                           com.trustplatform.member.TrustMemberRepository trustMemberRepository,
                           com.trustplatform.stories.SuccessStoryRepository successStoryRepository,
                           com.trustplatform.impact.ImpactShowcaseCardRepository impactShowcaseCardRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventRepository = eventRepository;
        this.donationRepository = donationRepository;
        this.volunteerRepository = volunteerRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
        this.impactStatRepository = impactStatRepository;
        this.trustMemberRepository = trustMemberRepository;
        this.successStoryRepository = successStoryRepository;
        this.impactShowcaseCardRepository = impactShowcaseCardRepository;
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

        // 4.5. Seed Mock Applicant User
        String applicantEmail = "applicant@trust.org";
        User applicantUser = null;
        if (!userRepository.existsByEmail(applicantEmail)) {
            logger.info("Seeding Applicant user account.");
            applicantUser = User.builder()
                    .fullName("Ramesh Kumar")
                    .email(applicantEmail)
                    .password(passwordEncoder.encode("applicant123"))
                    .role(Role.APPLICANT)
                    .isActive(true)
                    .build();
            applicantUser = userRepository.save(applicantUser);
        } else {
            applicantUser = userRepository.findByEmail(applicantEmail).orElse(null);
            if (applicantUser != null) {
                applicantUser.setPassword(passwordEncoder.encode("applicant123"));
                applicantUser.setRole(Role.APPLICANT);
                applicantUser.setActive(true);
                applicantUser = userRepository.save(applicantUser);
            }
            logger.info("Applicant user already exists. Force updated credentials.");
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

        // 10. Seed Core Impact Stats
        if (impactStatRepository.count() == 0) {
            logger.info("Seeding community impact statistics.");
            impactStatRepository.save(com.trustplatform.impact.ImpactStat.builder()
                    .category("WATER")
                    .currentValue(120L)
                    .unit("Filters Distributed")
                    .icon("water_drop")
                    .featured(true)
                    .displayOrder(1)
                    .build());

            impactStatRepository.save(com.trustplatform.impact.ImpactStat.builder()
                    .category("STUDENTS")
                    .currentValue(450L)
                    .unit("Children Coached")
                    .icon("school")
                    .featured(true)
                    .displayOrder(2)
                    .build());

            impactStatRepository.save(com.trustplatform.impact.ImpactStat.builder()
                    .category("TREES")
                    .currentValue(2500L)
                    .unit("Saplings Planted")
                    .icon("forest")
                    .featured(true)
                    .displayOrder(3)
                    .build());
        }

        // 11. Seed Team / Trust Members
        if (trustMemberRepository.count() == 0) {
            logger.info("Seeding key trust operational members.");
            trustMemberRepository.save(com.trustplatform.member.TrustMember.builder()
                    .name("Dr. K.V.G. Shanmuka Sai")
                    .role("Founder & Chairman")
                    .tagline("Dedicated to transforming lives through selfless service and community outreach.")
                    .bio("Dr. K.V.G. Shanmuka Sai is an eminent visionary and philanthropist with over 20 years of active social work experience, guiding education and environmental projects.")
                    .imageUrl("/hero-portrait.png")
                    .twitterUrl("https://twitter.com")
                    .linkedinUrl("https://linkedin.com")
                    .displayOrder(1)
                    .published(true)
                    .featured(true)
                    .build());

            trustMemberRepository.save(com.trustplatform.member.TrustMember.builder()
                    .name("Smt. K. Sarada")
                    .role("Managing Trustee")
                    .tagline("Empowering women and promoting clean water programs.")
                    .bio("Smt. K. Sarada manages the daily operations of the clean water distribution and student support systems, focusing on women empowerment.")
                    .imageUrl("/logo.png")
                    .twitterUrl("https://twitter.com")
                    .linkedinUrl("https://linkedin.com")
                    .displayOrder(2)
                    .published(true)
                    .featured(true)
                    .build());
        }

        // 12. Seed Success Stories
        if (successStoryRepository.count() == 0) {
            logger.info("Seeding historical NGO success stories.");
            successStoryRepository.save(com.trustplatform.stories.SuccessStory.builder()
                    .title("Transforming Suburbs through Pure Water")
                    .subtitle("Over 1,200 rural families received clean drinking water")
                    .description("The Pure Water Filtration Outreach project has successfully installed high-grade filtration kits, eradicating waterborne diseases in rural Nagpur suburbs.")
                    .category("Water Outreach")
                    .location("Nagpur Suburbs")
                    .imageUrl("https://images.unsplash.com/photo-1541252260730-0412e8e2108e?q=80&w=600")
                    .beforeImageUrl("https://images.unsplash.com/photo-1538300342682-ee57afb90d43?q=80&w=600")
                    .afterImageUrl("https://images.unsplash.com/photo-1541252260730-0412e8e2108e?q=80&w=600")
                    .published(true)
                    .featured(true)
                    .displayOrder(1)
                    .deleted(false)
                    .build());
        }

        // 13. Seed Showcase Cards
        if (impactShowcaseCardRepository.count() == 0) {
            logger.info("Seeding community impact showcase cards.");
            
            impactShowcaseCardRepository.save(com.trustplatform.impact.ImpactShowcaseCard.builder()
                    .title("Lighting the Path to Knowledge")
                    .subtitle("Education Initiative")
                    .description("Every child deserves a classroom, a mentor, and a future. Our education programs have placed thousands of rural children in structured learning environments — from primary schooling to vocational training.")
                    .metricCount("12,400+")
                    .statLabel("Children Educated")
                    .baseImage("/impact-gallery/gallery_education_base_1779805934541.png")
                    .revealImage("/impact-gallery/gallery_education_reveal_1779805956148.png")
                    .tags("Education, Rural Outreach, Youth")
                    .accentColor("rgba(245, 158, 11, 0.18)")
                    .displayOrder(1)
                    .build());

            impactShowcaseCardRepository.save(com.trustplatform.impact.ImpactShowcaseCard.builder()
                    .title("When Water Flows, Life Follows")
                    .subtitle("Clean Water Mission")
                    .description("Clean water transforms entire communities. We have installed hand pumps, purification units, and rainwater harvesting systems across 80+ villages — ending the daily walk for survival.")
                    .metricCount("80+")
                    .statLabel("Villages Reached")
                    .baseImage("/impact-gallery/gallery_water_base_1779805977777.png")
                    .revealImage("/impact-gallery/gallery_water_reveal_1779805998080.png")
                    .tags("Water, Sustainability, Infrastructure")
                    .accentColor("rgba(56, 189, 248, 0.18)")
                    .displayOrder(2)
                    .build());

            impactShowcaseCardRepository.save(com.trustplatform.impact.ImpactShowcaseCard.builder()
                    .title("Healing Where It Matters Most")
                    .subtitle("Healthcare Access")
                    .description("Our mobile medical camps and rural clinics have made primary healthcare accessible to the most remote communities — offering free checkups, medicines, and specialist consultations.")
                    .metricCount("38,000+")
                    .statLabel("Patients Treated")
                    .baseImage("/impact-gallery/gallery_health_base_1779806022982.png")
                    .revealImage("/impact-gallery/gallery_health_reveal_1779806043463.png")
                    .tags("Healthcare, Medical Camps, Wellness")
                    .accentColor("rgba(52, 211, 153, 0.18)")
                    .displayOrder(3)
                    .build());

            impactShowcaseCardRepository.save(com.trustplatform.impact.ImpactShowcaseCard.builder()
                    .title("Communities Built on Trust")
                    .subtitle("Rural Development")
                    .description("Sustainable change starts at the grassroots. We empower villages through infrastructure development, local governance support, and cultural programs that bring communities together.")
                    .metricCount("200+")
                    .statLabel("Communities Impacted")
                    .baseImage("/impact-gallery/gallery_community_base_1779806066469.png")
                    .revealImage("/impact-gallery/gallery_community_reveal_1779806091741.png")
                    .tags("Community, Development, Culture")
                    .accentColor("rgba(176, 122, 63, 0.18)")
                    .displayOrder(4)
                    .build());

            impactShowcaseCardRepository.save(com.trustplatform.impact.ImpactShowcaseCard.builder()
                    .title("Empowering Women, Uplifting Families")
                    .subtitle("Women's Empowerment")
                    .description("When women thrive, families flourish. Our self-help group network provides skills training, micro-financing, and mentorship — transforming vulnerable women into confident community leaders.")
                    .metricCount("5,200+")
                    .statLabel("Women Empowered")
                    .baseImage("/impact-gallery/gallery_women_base_1779806111914.png")
                    .revealImage("/impact-gallery/gallery_women_reveal_1779806136343.png")
                    .tags("Women, Empowerment, Livelihood")
                    .accentColor("rgba(232, 121, 249, 0.18)")
                    .displayOrder(5)
                    .build());
        }

        logger.info("[DataInitializer] Enterprise platform seeding execution successfully completed.");
    }
}
