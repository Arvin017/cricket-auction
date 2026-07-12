package com.auction.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = allowedOrigins.split(",");

        // Plain WebSocket endpoint (no SockJS needed for modern browsers),
        // but SockJS fallback is also registered under /ws-sockjs for
        // environments/proxies that block raw WebSocket upgrades.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins);

        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns(origins)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Clients SUBSCRIBE to topics under /topic (broadcast, one-to-many).
        // Room-wide auction state -> /topic/auction
        // Team-specific updates    -> /topic/team/{teamId}
        registry.enableSimpleBroker("/topic");

        // Clients SEND messages to destinations prefixed with /app; these
        // get routed to @MessageMapping methods in AuctionWebSocketController.
        registry.setApplicationDestinationPrefixes("/app");
    }
}
