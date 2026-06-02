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
    private final com.trustplatform.email.EmailService emailService;
    private final com.trustplatform.security.JwtService jwtService;
    private final com.trustplatform.auth.RefreshTokenService refreshTokenService;

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

    @GetMapping("/login/success")
    public void oauthSuccess(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.core.user.OAuth2User oauth2User,
            HttpServletResponse response) throws java.io.IOException {
        if (oauth2User == null) {
            response.sendRedirect(getRedirectBaseUrl() + "/login?error=oauth_failed");
            return;
        }

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");

        if (email == null || email.trim().isEmpty()) {
            response.sendRedirect(getRedirectBaseUrl() + "/login?error=oauth_failed");
            return;
        }

        try {
            com.trustplatform.user.User user = authService.oauthProvision(email, name);

            // Generate JWT and refresh tokens
            String accessToken = jwtService.generateToken(user);
            com.trustplatform.auth.RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            // Fallback avatar handling
            String avatarUrl = picture;
            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                avatarUrl = "https://ui-avatars.com/api/?name=" + java.net.URLEncoder.encode(user.getFullName(), java.nio.charset.StandardCharsets.UTF_8) + "&background=B07A3F&color=fff";
            }

            // Set cookie for automatic session validation
            setCookie(response, accessToken);

            // Redirect with credentials and avatar URL
            String redirectUrl = getRedirectBaseUrl() + "/login" +
                    "?token=" + accessToken +
                    "&refreshToken=" + refreshToken.getToken() +
                    "&name=" + java.net.URLEncoder.encode(user.getFullName(), java.nio.charset.StandardCharsets.UTF_8) +
                    "&email=" + user.getEmail() +
                    "&role=" + user.getRole().name() +
                    "&avatar=" + java.net.URLEncoder.encode(avatarUrl, java.nio.charset.StandardCharsets.UTF_8);

            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("Error during Google OAuth authentication redirect processing", e);
            response.sendRedirect(getRedirectBaseUrl() + "/login?error=oauth_failed");
        }
    }

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:kvgshanmukhsaitrust@gmail.com}")
    private String mailUsername;

    @GetMapping("/diag-email")
    public ResponseEntity<String> diagEmail(@RequestParam("to") String to) {
        try {
            emailService.sendEmailSync(to, "Production SMTP Diagnostic Test", "<h2>Production SMTP Test</h2><p>If you see this, SMTP is fully working!</p>");
            return ResponseEntity.ok("Success: SMTP is fully working! Email sent to " + to);
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            return ResponseEntity.status(500).body("SMTP Diagnostic Audit Fail!\n\n" +
                "Active Configured Settings:\n" +
                "• Host: " + mailHost + "\n" +
                "• Port: " + mailPort + "\n" +
                "• Username: " + mailUsername + "\n\n" +
                "Error Exception: " + e.getMessage() + "\n\n" +
                "Stacktrace:\n" + sw.toString());
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
