package com.example.flexbid.configuration;


import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
//import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	
	   
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // WebSocket endpoint
                .setAllowedOriginPatterns("*") // Allow all origins (use with caution in production)
                .withSockJS(); // Fallback for browsers without native WebSocket support
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Channels server can publish to
        config.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages from client to server (we won’t use it yet)
        config.setApplicationDestinationPrefixes("/app");

        // Prefix for user destination (private queue)
        config.setUserDestinationPrefix("/user");
    }
    
}
