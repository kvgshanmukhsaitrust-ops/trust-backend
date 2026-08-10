package com.trustplatform.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final com.trustplatform.security.JwtService jwtService;
    private final com.trustplatform.security.CustomUserDetailsService userDetailsService;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:*}")
    private String frontendUrl;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Handshake endpoint for WebSocket connection
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(frontendUrl)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix for public/broadcast (/topic) and user-specific (/queue) outbound messages
        registry.enableSimpleBroker("/topic", "/queue");
        
        // Prefix for inbound client-sent messages
        registry.setApplicationDestinationPrefixes("/app");
        
        // Prefix for user-targeted messages
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public org.springframework.messaging.Message<?> preSend(org.springframework.messaging.Message<?> message, org.springframework.messaging.MessageChannel channel) {
                org.springframework.messaging.simp.stomp.StompHeaderAccessor accessor =
                        org.springframework.messaging.support.MessageHeaderAccessor.getAccessor(message, org.springframework.messaging.simp.stomp.StompHeaderAccessor.class);
                
                if (accessor != null) {
                    if (org.springframework.messaging.simp.stomp.StompCommand.CONNECT.equals(accessor.getCommand())) {
                        String authHeader = accessor.getFirstNativeHeader("Authorization");
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            String token = authHeader.substring(7);
                            try {
                                String email = jwtService.extractUsername(token);
                                if (email != null) {
                                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                                    if (jwtService.isTokenValid(token, userDetails.getUsername())) {
                                        org.springframework.security.authentication.UsernamePasswordAuthenticationToken authToken =
                                                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                                        userDetails, null, userDetails.getAuthorities());
                                        accessor.setUser(authToken);
                                        log.info("[WebSocketConfig] WebSocket CONNECT frame authenticated successfully for user: {}", email);
                                    }
                                }
                            } catch (Exception e) {
                                log.error("[WebSocketConfig] WebSocket connection authentication failed: {}", e.getMessage());
                                throw new org.springframework.messaging.MessageDeliveryException(message, "Unauthorized: Token validation failed");
                            }
                        }
                    } else if (org.springframework.messaging.simp.stomp.StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                        String destination = accessor.getDestination();
                        java.security.Principal principal = accessor.getUser();
                        
                        if (destination != null) {
                            // If destination targets admin, restrict to users with admin/officer rights
                            if (destination.startsWith("/topic/admin") || destination.startsWith("/queue/admin")) {
                                if (principal == null) {
                                    log.warn("[WebSocketConfig] WebSocket subscription blocked: Anonymous user attempted to subscribe to admin topic: {}", destination);
                                    throw new org.springframework.security.access.AccessDeniedException("Access denied: Administrative subscriptions require authentication");
                                }
                                
                                org.springframework.security.core.Authentication auth = (org.springframework.security.core.Authentication) principal;
                                boolean isAuthorized = auth.getAuthorities().stream()
                                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                                                || "MANAGE_APPLICATIONS".equals(a.getAuthority())
                                                || "VIEW_ANALYTICS".equals(a.getAuthority()));
                                
                                if (!isAuthorized) {
                                    log.warn("[WebSocketConfig] WebSocket subscription blocked: User {} lacks authority for admin topic: {}", principal.getName(), destination);
                                    throw new org.springframework.security.access.AccessDeniedException("Access denied: You lack permissions for admin subscriptions");
                                }
                                log.info("[WebSocketConfig] WebSocket subscription authorized to admin topic: {} for user: {}", destination, principal.getName());
                            }
                        }
                    }
                }
                return message;
            }
        });
    }
}
