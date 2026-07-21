package com.trustplatform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String COOKIE_NAME = "access_token";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if ("websocket".equalsIgnoreCase(request.getHeader("Upgrade")) && request.getRequestURI().startsWith("/ws")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = extractToken(request);

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtService.extractUsername(jwt);
            if (email != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);
                if (jwtService.isTokenValid(jwt, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null,
                                    userDetails.getAuthorities());
                    authToken.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    sendErrorResponse(response, "Invalid token");
                    return;
                }
            } else {
                sendErrorResponse(response, "Invalid token claims");
                return;
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
            sendErrorResponse(response, "JWT token has expired");
            return;
        } catch (io.jsonwebtoken.security.SignatureException | io.jsonwebtoken.MalformedJwtException e) {
            log.debug("JWT signature check/format failed: {}", e.getMessage());
            sendErrorResponse(response, "JWT token signature is invalid or malformed");
            return;
        } catch (Exception e) {
            log.debug("JWT filter exception: {}", e.getMessage());
            sendErrorResponse(response, "Authentication failed");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format("{\"success\":false,\"message\":\"%s\",\"status\":401}", message));
    }

    private String extractToken(HttpServletRequest request) {
        String headerToken = extractFromHeader(request);
        if (headerToken != null) {
            return headerToken;
        }
        
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(c -> COOKIE_NAME.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private String extractFromHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}