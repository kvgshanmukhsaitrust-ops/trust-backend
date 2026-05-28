package com.trustplatform.notification;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

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
}
