package com.example.flexbid.controller;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.flexbid.service.ReverseBidService;

@RestController
@RequestMapping("/api/reverse-bids")
@CrossOrigin(origins="*")
public class ReverseBidController {

    @Autowired
    private ReverseBidService reverseBidService;
    
    @PostMapping("/placereversebid/{requestId}")
    public ResponseEntity<?> placeReverseBid(
            @PathVariable int requestId,
            @RequestParam BigDecimal bidAmount,
            @RequestParam int sellerId
    ){
    	return reverseBidService.placeReverseBid(requestId, sellerId, bidAmount);
    }

    @GetMapping("/bids/{requestId}")
    public ResponseEntity<?> getBidsForRequest(@PathVariable int requestId) {
        return reverseBidService.getBidsForRequest(requestId);
    }

    @GetMapping("/finalized")
    public ResponseEntity<?> getFinalizedResults() {
        return reverseBidService.getFinalizedResults();
    }
    
    @PostMapping("/manuallyFinalize/{requestId}")
    public ResponseEntity<?> manuallyFinalize(@PathVariable int requestId){
    	return reverseBidService.manuallyFinalize(requestId);
    }
    
    @GetMapping("/winner/seller/{sellerId}")
    public ResponseEntity<?> getWinnerForRequest(@PathVariable int sellerId){
    	return reverseBidService.getWinningRequestsForSeller(sellerId);
    }
    
    @PutMapping("/{buyerId}/complete-service/{requestId}")
    public ResponseEntity<?> completeService(
            @PathVariable int buyerId,
            @PathVariable int requestId) {
        return reverseBidService.markServiceAsCompleted(requestId, buyerId);
    }
    
}

