package com.example.flexbid.controller;

import java.math.BigDecimal;
import java.util.Collections;
//import java.util.Optional;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
//import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.flexbid.dto.Winner;
//import com.example.flexbid.model.BiddingTimeSlot;
import com.example.flexbid.service.BidService;

//import okhttp3.Response;

@RestController
@RequestMapping("/api/bids")
@CrossOrigin(origins = "*")
public class BidController {

    @Autowired
    private BidService bidService;
    

    // 1. Place a new bid
    @PostMapping("/placebid")
    public ResponseEntity<?> placeBid(
            @RequestParam int buyerId,
            @RequestParam int productId,
            @RequestParam(required = false) BigDecimal bidAmount,
            @RequestParam(defaultValue = "false") boolean isIncrement) {
        return bidService.placeBid(buyerId, productId, bidAmount, isIncrement);
    }

    // 2. Get all bids for a product
    @GetMapping("/history/{productId}")
    public ResponseEntity<?> getAllBidsForProduct(@PathVariable int productId) {
        return bidService.getAllBidsForProduct(productId);
    }

    /**
     * Endpoint for a buyer to withdraw from an auction
     * @param buyerId ID of the buyer
     * @param productId ID of the product (auction)
     * @return ResponseEntity with status and message
     */
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdrawFromAuction(
            @RequestParam int buyerId,
            @RequestParam int productId
    ) {
        return bidService.withdrawFromAuction(buyerId, productId);
    }
    
//    @GetMapping("/state/{productId}")
//    public ResponseEntity<?> getBiddingState(@PathVariable int productId) {
//        return bidService.getBiddingState(productId);
//    }

    // 4. Finalize winner for a product
    @GetMapping("/winnerdetails/{productId}")
    public ResponseEntity<?> getFinalizedBidDetails(@PathVariable int productId) {
        return bidService.getFinalizedBidDetails(productId);
    }
    
//    @PostMapping("/reschedule-unbidded")
//    public ResponseEntity<String> triggerRescheduling() {
//    	bidService.rescheduleUnbiddedProducts();
//        return ResponseEntity.ok("Rescheduling triggered manually.");
//    }
    
    @GetMapping("/{buyerId}/winning-bids")
    public ResponseEntity<?> getWinningBids(@PathVariable int buyerId) {
        return bidService.getWinningBidsByBuyerId(buyerId);
    }
    
    @GetMapping("/winners")
    public ResponseEntity<List<Winner>> getAllWinners() {
        List<Winner> winners = bidService.getAllWinners();
        if (winners.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        }
        return ResponseEntity.ok(winners);
    }

    // 2. Get all winners for a specific product
    @GetMapping("/winners/{productId}")
    public ResponseEntity<?> getWinnersForProduct(@PathVariable int productId) {
        return bidService.getAllWinnersForProduct(productId);
    }
    
    @GetMapping("/liveBiddingProducts")
    public ResponseEntity<?> getLiveProducts(){
    	return bidService.getLiveBiddingProducts();
    }
    
    @PostMapping("/reschedule-unbidded")
    public ResponseEntity<?> manuallyRescheduleUnbiddedProducts() {
    	bidService.rescheduleUnbiddedProducts();
        return ResponseEntity.ok("Unbidded product rescheduling process triggered successfully.");
    }
}