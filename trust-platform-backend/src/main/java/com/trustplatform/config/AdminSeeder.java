package com.trustplatform.config;

import com.trustplatform.user.Role;
import com.trustplatform.user.User;
import com.trustplatform.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

// SECURITY: creates admin@trust.org / admin123 (and prints it). Development only.
@org.springframework.context.annotation.Profile("!prod")
@Configuration
public class AdminSeeder {

    @Bean
    public CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail("admin@trust.org").isEmpty()) {
                User admin = new User();
                admin.setFullName("Platform Admin");
                admin.setEmail("admin@trust.org");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                admin.setActive(true);
                userRepository.save(admin);
                System.out.println("====== ADMIN ACCOUNT CREATED ======");
                System.out.println("Email: admin@trust.org");
                System.out.println("Password: admin123");
                System.out.println("===================================");
            }
        };
    }
}
