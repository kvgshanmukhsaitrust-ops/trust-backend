package com.trustplatform.auth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @Value("${app.jwt.expiration:900000}")
    private long accessTokenExpirationMs;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    private String getRedirectBaseUrl() {
        if (frontendUrl != null && !frontendUrl.trim().isEmpty() && !"*".equals(frontendUrl.trim())) {
            String[] urls = frontendUrl.split(",");
            return urls[0].trim();
        }
        try {
            String backendUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder
                    .fromCurrentContextPath().build().toUriString();
            if (backendUrl != null && (backendUrl.contains("railway.app") || !backendUrl.contains("localhost"))) {
                return "https://trust-frontend-delta.vercel.app";
            }
        } catch (Exception e) {
            // Fallback in non-web contexts
        }
        return "http://localhost:5173";
    }

    @GetMapping("/verify")
    public void verifyEmail(@RequestParam("token") String token, HttpServletResponse response) throws java.io.IOException {
        try {
            emailVerificationService.verifyToken(token);
            log.info("Email verified successfully for token: {}", token);
            response.sendRedirect(getRedirectBaseUrl() + "/login?verified=true");
        } catch (Exception e) {
            log.error("Email verification failed for token: {}", token, e);
            response.sendRedirect(getRedirectBaseUrl() + "/login?error=invalid");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody AuthenticationRequest request,
            HttpServletResponse response) {
        AuthenticationResponse auth = authService.login(request);
        if (auth.getToken() != null) setCookie(response, auth.getToken());
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(
            @RequestBody RefreshTokenRequest request,
            HttpServletResponse response) {
        AuthenticationResponse auth = authService.refreshToken(request);
        if (auth.getToken() != null) setCookie(response, auth.getToken());
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletResponse response) {
        if (request != null && request.getRefreshToken() != null) {
            authService.logout(request.getRefreshToken());
        }
        clearCookie(response);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    private void setCookie(HttpServletResponse response, String token) {
        String cookie = String.format(
            "access_token=%s; Max-Age=%d; Path=/api; HttpOnly; %s SameSite=Strict",
            token,
            (int)(accessTokenExpirationMs / 1000),
            secureCookie ? "Secure;" : "");
        response.addHeader("Set-Cookie", cookie);
    }

    private void clearCookie(HttpServletResponse response) {
        String cookie = String.format(
            "access_token=; Max-Age=0; Path=/api; HttpOnly; %s SameSite=Strict",
            secureCookie ? "Secure;" : "");
        response.addHeader("Set-Cookie", cookie);
    }
}
