package com.example.flexbid.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.flexbid.service.OrderService;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins="*")
public class PaymentController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/create-razorpay-order/{orderId}")
    public ResponseEntity<?> createRazorpayOrder(@PathVariable Integer orderId) {
        return orderService.createRazorpayOrder(orderId);
    }
    
}
