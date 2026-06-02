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

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:*}")
    private String frontendUrl;

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
                        .requestMatchers("/api/auth/**", "/api/payments/webhook", "/error", "/uploads/**", "/ws/**", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/donations").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/create-order/**", "/api/payments/verify").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/donations/*/receipt").permitAll()

                        // Public read-only content
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/impact-stats/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/success-stories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/members/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/media/**").permitAll()

                        // Public contact form
                        .requestMatchers(HttpMethod.POST, "/api/messages").permitAll()

                        // Admin-only: content mutations + admin namespace (delegated to @PreAuthorize on controllers)
                        .requestMatchers(HttpMethod.POST,   "/api/events/**").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/api/events/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH,  "/api/events/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/events/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/api/success-stories/**").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/api/success-stories/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH,  "/api/success-stories/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/success-stories/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/api/members/**").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/api/members/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH,  "/api/members/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/members/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/api/impact-stats/**").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/api/impact-stats/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH,  "/api/impact-stats/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/impact-stats/**").authenticated()
                        .requestMatchers("/api/admin/**").authenticated()

                        // Volunteer: any authenticated user can apply
                        .requestMatchers(HttpMethod.POST, "/api/volunteers/apply").authenticated()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                .oauth2Login(oauth -> oauth.defaultSuccessUrl("/api/auth/login/success", true))

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
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

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        
        java.util.List<String> patterns = new java.util.ArrayList<>();
        patterns.add("https://trust-frontend-delta.vercel.app");
        patterns.add("http://localhost:5173");
        patterns.add("http://localhost:3000");
        
        if (frontendUrl != null && !frontendUrl.trim().isEmpty()) {
            for (String origin : frontendUrl.split(",")) {
                String trimmed = origin.trim();
                if (!patterns.contains(trimmed)) {
                    patterns.add(trimmed);
                }
            }
        }
        
        configuration.setAllowedOriginPatterns(patterns);
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(true);
        
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}