package com.trustplatform.config;

import com.trustplatform.security.JwtAuthenticationFilter;
import com.trustplatform.security.RateLimitingFilter;
import com.trustplatform.security.CustomAccessDeniedHandler;
import com.trustplatform.security.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())

                // =============================================
                // SECURITY HEADERS
                // =============================================
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                        "script-src 'self'; " +
                                        "frame-ancestors 'none'"
                                )
                        )
                )

                // =============================================
                // AUTHORIZATION RULES
                // =============================================
                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints — always public
                        .requestMatchers("/api/auth/**", "/api/payments/webhook", "/error", "/uploads/**").permitAll()

                        // Public read-only content
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/impact-stats/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/success-stories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/members/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()

                        // Public contact form
                        .requestMatchers(HttpMethod.POST, "/api/messages").permitAll()

                        // Admin-only: content mutations + admin namespace
                        .requestMatchers(HttpMethod.POST,   "/api/events/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/events/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH,  "/api/events/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/events/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/success-stories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/success-stories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH,  "/api/success-stories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/success-stories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/members/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/members/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH,  "/api/members/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/members/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/impact-stats/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/impact-stats/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH,  "/api/impact-stats/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/impact-stats/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Volunteer: any authenticated user can apply
                        .requestMatchers(HttpMethod.POST, "/api/volunteers/apply").authenticated()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // =============================================
                // FILTER ORDER:
                // RateLimitingFilter → JwtAuthenticationFilter
                // Rate limiting fires first on /api/auth/**
                // =============================================
                .addFilterBefore(rateLimitingFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength 12 — current production minimum (was default 10)
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}