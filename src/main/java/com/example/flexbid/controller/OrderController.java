package com.example.flexbid.controller;

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

import com.example.flexbid.dto.PaymentVerificationRequest;
import com.example.flexbid.dto.PlaceOrderRequest;
import com.example.flexbid.service.OrderService;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins="*")
public class OrderController {

	@Autowired
	private OrderService orderService;
	
	
	@PostMapping("/place")
	public ResponseEntity<?> placeOrder(@RequestBody PlaceOrderRequest request) {
	    return orderService.placeOrder(request);
	}
	
	@PostMapping("/verify-payment")
	public ResponseEntity<?> verifyPayment(@RequestBody PaymentVerificationRequest request) {
	    return orderService.verifyAndConfirmPayment(request);
	}

	    @PostMapping("/{orderId}/ship")
	    public ResponseEntity<?> markAsShipped(@PathVariable Integer orderId,
	                                           @RequestParam Integer sellerId) {
	        return orderService.markOrderAsShipped(orderId, sellerId);
	    }

	    @PostMapping("/{orderId}/deliver")
	    public ResponseEntity<?> markAsDeliveredByBuyer(@PathVariable Integer orderId,
	                                                    @RequestParam Integer buyerId) {
	        return orderService.markOrderAsDeliveredByBuyer(orderId, buyerId);
	    }

	    @PostMapping("/{orderId}/cancel")
	    public ResponseEntity<?> cancelOrder(@PathVariable Integer orderId,
	                                         @RequestParam Integer userId) {
	        return orderService.cancelOrder(orderId, userId);
	    }

	    @PostMapping("/{orderId}/return")
	    public ResponseEntity<?> returnOrder(@PathVariable Integer orderId,
	                                         @RequestParam Integer userId) {
	        return orderService.returnOrder(orderId, userId);
	    }

	    @PostMapping("/{orderId}/replace")
	    public ResponseEntity<?> replaceOrder(@PathVariable Integer orderId,
	                                          @RequestParam Integer userId,
	                                          @RequestParam String reason) {
	        return orderService.replaceOrder(orderId, userId,reason);
	    }
	    
	    @PutMapping("/update-status")
	    public ResponseEntity<?> updatePaymentStatusAfterRefund(@RequestParam Integer paymentId) {
	        return orderService.updatePaymentStatusAfterRefund(paymentId);
	    }
	    
	    @PutMapping("/admin/return/update-refund-status")
	    public ResponseEntity<?> updateReturnRefundStatus(@RequestParam Integer paymentId) {
	        return orderService.updateReturnRefundStatus(paymentId);
	    }
	    
	    @PutMapping("/admin/order/update-replacement-status")
	    public ResponseEntity<?> updateReplacementStatus(@RequestParam Integer orderId) {
	        return orderService.updateReplacementStatus(orderId);
	    }

//	    @PostMapping("/{orderId}/confirm-delivery")
//	    public ResponseEntity<?> confirmDelivery(@PathVariable Integer orderId,
//	                                             @RequestParam Integer userId) {
//	        return orderService.markOrderAsDelivered(orderId, userId);
//	    }
	
	    @GetMapping("/buyer/{buyerId}")
	    public ResponseEntity<?> getOrdersByBuyer(@PathVariable int buyerId) {
	        return orderService.getOrdersByBuyerId(buyerId);
	    }

	    @GetMapping("/seller/{sellerId}")
	    public ResponseEntity<?> getOrdersBySeller(@PathVariable int sellerId) {
	        return orderService.getOrdersBySellerId(sellerId);
	    }
	    
	    @GetMapping("/orders")
	    public ResponseEntity<?> getAllOrders() {
	        return orderService.getAllOrders();
	    }

	    
}
