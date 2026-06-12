package com.example.flexbid.controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;
import com.example.flexbid.dto.ServiceRequest;
import com.example.flexbid.service.BuyerService;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins="*")
public class BuyerController {

    @Autowired
    private BuyerService buyerService;
    
//    @Autowired
//    private ProductService productService;

    @PostMapping("/buyer/{buyerId}/request")
    public ResponseEntity<?> createRequest(@RequestBody ServiceRequest request, @PathVariable int buyerId) {
        return buyerService.createServiceRequest(request, buyerId);
    }
    
    @GetMapping("/buyer/{buyerId}/requests")
    public ResponseEntity<?> getBuyerServiceHistory(@PathVariable int buyerId) {
        return buyerService.getServiceRequestHistoryByBuyer(buyerId);
    }

    @GetMapping("/seller/open-requests")
    public ResponseEntity<?> getOpenRequestsForSellers() {
        return buyerService.getAllOpenServiceRequestsForSellers();
    }
    
    @GetMapping("/buyer/request/{requestId}")
    public ResponseEntity<?> getRequestById(
            @PathVariable int requestId
    ) {
        return buyerService.getRequestById(requestId);
    }
   
    @GetMapping("/stats/{buyerId}")
    public ResponseEntity<?> getBuyerStats(@PathVariable int buyerId) {
        return buyerService.getBuyerStats(buyerId);
    }
    
    @PutMapping("/recheduleDate/{requestId}")
    public ResponseEntity<?> rescheduleDate(@PathVariable int requestId, @RequestBody LocalDate rescheduleDate){
    	return buyerService.requestRescheduleDate(requestId, rescheduleDate);
    }

    @PostMapping("/{requestId}/respond-reschedule")
    public ResponseEntity<?> respondToReschedule(
            @PathVariable int requestId,
            @RequestParam boolean accepted
    ) {
        return buyerService.respondToRescheduleDate(requestId, accepted);
    }
}
