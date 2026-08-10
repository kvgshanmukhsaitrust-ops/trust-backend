package com.trustplatform.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    @Value("${rate-limit.auth.capacity:10}")
    private int capacity;

    @Value("${rate-limit.auth.refill-tokens:10}")
    private int refillTokens;

    @Value("${rate-limit.auth.refill-seconds:60}")
    private int refillSeconds;

    @Value("${rate-limit.donation.capacity:5}")
    private int donationCapacity;

    @Value("${rate-limit.upload.capacity:3}")
    private int uploadCapacity;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (!isRateLimitedRoute(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String routeType = getRouteType(request);
        String ip = resolveIp(request);
        String key = ip + ":" + routeType;

        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucketForRoute(routeType));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded: ip={}, key={}, uri={}", ip, key, uri);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                "{\"status\":429,\"title\":\"Too Many Requests\"," +
                "\"detail\":\"Max attempts exceeded for this route. Please try again later.\"}");
        }
    }

    private boolean isRateLimitedRoute(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (uri.startsWith("/api/auth/")) {
            return true;
        }
        if (uri.equals("/api/donations") && "POST".equalsIgnoreCase(method)) {
            return true;
        }
        if (uri.startsWith("/api/payments/create-order/") && "POST".equalsIgnoreCase(method)) {
            return true;
        }
        if (uri.equals("/api/media/upload") && "POST".equalsIgnoreCase(method)) {
            return true;
        }
        return false;
    }

    private String getRouteType(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (uri.equals("/api/donations") && "POST".equalsIgnoreCase(method)) {
            return "DONATION";
        }
        if (uri.startsWith("/api/payments/create-order/") && "POST".equalsIgnoreCase(method)) {
            return "DONATION";
        }
        if (uri.equals("/api/media/upload") && "POST".equalsIgnoreCase(method)) {
            return "UPLOAD";
        }
        return "AUTH";
    }

    private Bucket createBucketForRoute(String routeType) {
        int cap = capacity;
        int refill = refillTokens;
        int seconds = refillSeconds;

        if ("DONATION".equals(routeType)) {
            cap = donationCapacity;
            refill = donationCapacity;
            seconds = 60;
        } else if ("UPLOAD".equals(routeType)) {
            cap = uploadCapacity;
            refill = uploadCapacity;
            seconds = 60;
        }

        return Bucket.builder()
                .addLimit(Bandwidth.classic(cap,
                        Refill.greedy(refill,
                                Duration.ofSeconds(seconds))))
                .build();
    }

    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();
        return request.getRemoteAddr();
    }
}