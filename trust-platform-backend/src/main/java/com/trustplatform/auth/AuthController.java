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

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

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
                return "https://shanmukasaitrust.org";
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
            String maskedToken = token.length() > 6 ? token.substring(0, 6) + "..." : "...";
            log.info("Email verified successfully for token: {}", maskedToken);
            response.sendRedirect(getRedirectBaseUrl() + "/login?verified=true");
        } catch (Exception e) {
            String maskedToken = token.length() > 6 ? token.substring(0, 6) + "..." : "...";
            log.error("Email verification failed for token: {}", maskedToken, e);
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

            // Set HttpOnly cookies for secure session management
            setCookie(response, accessToken);
            setRefreshCookie(response, refreshToken.getToken());

            // Redirect indicating OAuth success without token exposure in the URL
            String redirectUrl = getRedirectBaseUrl() + "/login?oauth_success=true";

            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("Error during Google OAuth authentication redirect processing", e);
            response.sendRedirect(getRedirectBaseUrl() + "/login?error=oauth_failed");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<com.trustplatform.common.api.ApiSuccessResponse<AuthenticationResponse.UserDto>> getMe(
            org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new com.trustplatform.exception.UnauthorizedException("Not authenticated");
        }
        com.trustplatform.user.User user = (com.trustplatform.user.User) authentication.getPrincipal();
        AuthenticationResponse.UserDto userDto = AuthenticationResponse.UserDto.builder()
                .name(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
        return ResponseEntity.ok(
                com.trustplatform.common.api.ApiSuccessResponse.<AuthenticationResponse.UserDto>builder()
                        .timestamp(java.time.LocalDateTime.now())
                        .status(200)
                        .message("Session retrieved successfully")
                        .data(userDto)
                        .build()
        );
    }


    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthenticationResponse auth = authService.register(request);
        if (auth.getToken() != null) setCookie(response, auth.getToken());
        if (auth.getRefreshToken() != null) setRefreshCookie(response, auth.getRefreshToken());
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody AuthenticationRequest request,
            HttpServletResponse response) {
        AuthenticationResponse auth = authService.login(request);
        if (auth.getToken() != null) setCookie(response, auth.getToken());
        if (auth.getRefreshToken() != null) setRefreshCookie(response, auth.getRefreshToken());
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(name = "refresh_token", required = false) String refreshTokenFromCookie,
            HttpServletResponse response) {
        
        String token = (request != null && request.getRefreshToken() != null) 
                ? request.getRefreshToken() 
                : refreshTokenFromCookie;

        if (token == null || token.trim().isEmpty()) {
            throw new com.trustplatform.exception.BadRequestException("Missing refresh token");
        }

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(token);
        AuthenticationResponse auth = authService.refreshToken(refreshRequest);
        if (auth.getToken() != null) setCookie(response, auth.getToken());
        if (auth.getRefreshToken() != null) setRefreshCookie(response, auth.getRefreshToken());
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(name = "refresh_token", required = false) String refreshTokenFromCookie,
            HttpServletResponse response) {
        
        String token = (request != null && request.getRefreshToken() != null) 
                ? request.getRefreshToken() 
                : refreshTokenFromCookie;

        if (token != null && !token.trim().isEmpty()) {
            authService.logout(token);
        }
        clearCookie(response);
        clearRefreshCookie(response);
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

    private void setRefreshCookie(HttpServletResponse response, String token) {
        String cookie = String.format(
            "refresh_token=%s; Max-Age=%d; Path=/api; HttpOnly; %s SameSite=Strict",
            token,
            (int)(refreshExpirationMs / 1000),
            secureCookie ? "Secure;" : "");
        response.addHeader("Set-Cookie", cookie);
    }

    private void clearCookie(HttpServletResponse response) {
        String cookie = String.format(
            "access_token=; Max-Age=0; Path=/api; HttpOnly; %s SameSite=Strict",
            secureCookie ? "Secure;" : "");
        response.addHeader("Set-Cookie", cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        String cookie = String.format(
            "refresh_token=; Max-Age=0; Path=/api; HttpOnly; %s SameSite=Strict",
            secureCookie ? "Secure;" : "");
        response.addHeader("Set-Cookie", cookie);
    }
}
