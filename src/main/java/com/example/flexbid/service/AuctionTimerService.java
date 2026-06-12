package com.example.flexbid.service;

import org.springframework.stereotype.Service;

@Service
public class AuctionTimerService {

    public void restartTimer(int productId) {
        // Restart timer logic for the auction, e.g., via WebSocket or scheduler
        System.out.println("Restarting auction timer for product " + productId);
    }

    public void cancelTimer(int productId) {
        // Cancel timer or mark auction inactive
        System.out.println("Cancelling auction timer for product " + productId);
    }
}