package com.example.flexbid.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 🔔 Broadcast to all users subscribed to general notifications
     */
    public void broadcast(Object payload) {
        messagingTemplate.convertAndSend("/topic/notifications", payload);
        System.out.println("📢 Broadcast sent to /topic/notifications: " + payload);
    }

    /**
     * 📬 Send a private message to a specific user (by email/username)
     */
    public void sendToUser(String emailOrUsername, Object payload) {
        messagingTemplate.convertAndSendToUser(emailOrUsername, "/queue/private", payload);
        System.out.println("📩 Private message sent to " + emailOrUsername + " @ /queue/private: " + payload);
    }

    /**
     * 🔔 Send to any dynamic topic (e.g., /topic/products)
     */
    public void notifyTopic(String topicSuffix, Object payload) {
        messagingTemplate.convertAndSend("/topic/" + topicSuffix, payload);
        System.out.println("📢 Broadcast sent to /topic/" + topicSuffix + ": " + payload);
    }

    /**
     * 📧 Specialized email verified notification
     */
    public void sendEmailVerifiedNotification(String email) {
        Map<String, String> payload = new HashMap<>();
        payload.put("type", "EMAIL_VERIFIED");
        sendToUser(email, payload);
    }
    
    public void broadcastToTopic(String topic, Object payload) {
        messagingTemplate.convertAndSend(topic, payload);
    }
}
