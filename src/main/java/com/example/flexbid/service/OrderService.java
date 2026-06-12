package com.example.flexbid.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
//import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flexbid.dto.PaymentVerificationRequest;
import com.example.flexbid.dto.PlaceOrderRequest;
import com.example.flexbid.dto.RefundResponseDTO;
import com.example.flexbid.model.Bid;
import com.example.flexbid.model.DeliveryStatus;
import com.example.flexbid.model.Order;
import com.example.flexbid.model.OrderStatus;
import com.example.flexbid.model.Payment;
import com.example.flexbid.model.PaymentStatus;
import com.example.flexbid.model.Product;
import com.example.flexbid.model.ProductImage;
import com.example.flexbid.model.RequestStatus;
import com.example.flexbid.model.User;
import com.example.flexbid.repository.BidRepository;
import com.example.flexbid.repository.OrderRepository;
import com.example.flexbid.repository.PaymentRepository;
import com.example.flexbid.repository.ProductImageRepository;
import com.example.flexbid.repository.ProductRepository;
import com.example.flexbid.repository.UserRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RazorpayClientProvider razorpayClientProvider;
    
    @Autowired
    private RazorpayClient razorpayClient;
    
    @Autowired
	private WebSocketNotificationService notificationService;
    
    @Autowired
    private ProductImageRepository productImageRepo;
    
    @Autowired
    private EmailService emailService;


    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret}")
    private String razorpayKeySecret;


    @Transactional
    public ResponseEntity<?> placeOrder(PlaceOrderRequest request) {
        // 1. Fetch & validate bid
        Optional<Bid> bidOpt = bidRepository.findById(request.getBidId());
        if (bidOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid bid ID.");
        }

        Bid bid = bidOpt.get();

        if (!bid.isWinningBid()) {
            return ResponseEntity.badRequest().body("Cannot place order. This is not a winning bid.");
        }

        // 2. Fetch & validate user and product
        Optional<User> userOpt = userRepository.findById(bid.getBuyerId());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found.");
        }

        Optional<Product> productOpt = productRepository.findById(bid.getProductId());
        if (productOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Product not found.");
        }

        // 3. Validate form fields
        String billingName = request.getBillingName();
        String contactNumber = request.getContactNumber();
        String address = request.getAddress();

        if (billingName == null || billingName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Billing name is required.");
        }

        if (contactNumber == null || !contactNumber.trim().matches("\\d{10}")) {
            return ResponseEntity.badRequest().body("Contact number must be exactly 10 digits.");
        }

        if (address == null || address.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Address is required.");
        }

        // 4. Prevent duplicate order for same bid
        if (orderRepository.existsByBid(bid)) {
            return ResponseEntity.badRequest().body("Order already placed for this bid.");
        }

        // 5. Create and save order
        User buyer = userOpt.get();
        Product product = productOpt.get();
        BigDecimal totalAmount = bid.getAmount();

        Order order = new Order();
        order.setUser(buyer);
        order.setBid(bid);
        order.setProduct(product);
        order.setBillingName(billingName.trim());
        order.setContactNumber(contactNumber.trim());
        order.setAddress(address.trim());
        order.setQuantity(1);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryStatus(DeliveryStatus.PLACED);
        order.setCreatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        // 6. Create pending payment
        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 7. WebSocket notify user
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ORDER_PLACED");
        payload.put("orderId", savedOrder.getId());
        payload.put("productId", product.getId());
        payload.put("amount", totalAmount);
        payload.put("status", order.getStatus());
        payload.put("biddingRound", bidOpt.get().getBiddingRound());
        payload.put("timestamp", LocalDateTime.now().toString());

        notificationService.sendToUser(buyer.getUsername(), payload);

        return ResponseEntity.ok(savedOrder);
    }


    @Transactional
    public ResponseEntity<?> createRazorpayOrder(Integer orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid order ID.");
        }

        Order order = orderOpt.get();

        if (!OrderStatus.PENDING.equals(order.getStatus())) {
            return ResponseEntity.badRequest().body("Order is already processed or cancelled.");
        }

        BigDecimal amount = order.getTotalAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("Invalid order amount.");
        }

        try {
            int amountInPaise = amount.multiply(BigDecimal.valueOf(100)).intValue(); // Razorpay needs paise

            RazorpayClient razorpay = razorpayClientProvider.getClient();

            JSONObject options = new JSONObject();
            options.put("amount", amountInPaise);
            options.put("currency", "INR");
            options.put("receipt", "order_rcptid_" + orderId);
            options.put("payment_capture", 1);

            com.razorpay.Order razorpayOrder = razorpay.orders.create(options);

            Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new IllegalStateException("Payment record not found for order ID: " + orderId));

            payment.setRazorpayOrderId(razorpayOrder.get("id"));
            payment.setPaymentMethod("razorpay");
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // ✅ WebSocket: Notify user about Razorpay order
            Map<String, Object> wsPayload = new HashMap<>();
            wsPayload.put("type", "RAZORPAY_ORDER_CREATED");
            wsPayload.put("razorpayOrderId", razorpayOrder.get("id"));
            wsPayload.put("amount", amountInPaise);
            wsPayload.put("orderId", order.getId());
            notificationService.sendToUser(order.getUser().getUsername(), wsPayload);

            // ✅ Prepare frontend response
            Map<String, Object> response = new HashMap<>();
            response.put("razorpayOrderId", razorpayOrder.get("id"));
            response.put("amount", amountInPaise);
            response.put("currency", "INR");
            response.put("orderId", order.getId());
            response.put("key", razorpayKeyId); // Frontend uses this public key

            return ResponseEntity.ok(response);

        } catch (RazorpayException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Razorpay order creation failed: " + e.getMessage());
        }
    }

    @Transactional
    public ResponseEntity<?> verifyAndConfirmPayment(PaymentVerificationRequest request) {
        try {
            Optional<Payment> paymentOpt = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId());
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid Razorpay Order ID.");
            }

            Payment payment = paymentOpt.get();
            Order order = payment.getOrder();

            // Check for duplicate confirmation
            if (!OrderStatus.PENDING.equals(order.getStatus()) || !PaymentStatus.PENDING.equals(payment.getPaymentStatus())) {
                return ResponseEntity.badRequest().body("Payment is already processed.");
            }

            // Generate signature and compare
            String generatedSignature = generateRazorpaySignature(
                request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId(),
                razorpayKeySecret
            );

            if (!generatedSignature.equals(request.getRazorpaySignature())) {
                return ResponseEntity.badRequest().body("❌ Payment signature verification failed.");
            }

            // ✅ Update payment and order status
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            order.setStatus(OrderStatus.CONFIRMED);
            order.setCreatedAt(LocalDateTime.now());
            orderRepository.save(order);
            
            // ✅ Send order success email with PDF
            emailService.sendOrderSuccessEmail(order, payment);

            // ✅ Send WebSocket notification
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "PAYMENT_CONFIRMED");
            payload.put("orderId", order.getId());
            payload.put("productId", order.getProduct().getId());
            payload.put("amount", order.getTotalAmount());
            payload.put("status", order.getStatus());
            payload.put("timestamp", LocalDateTime.now().toString());

            notificationService.sendToUser(order.getUser().getUsername(), payload);

            return ResponseEntity.ok("✅ Payment verified and order confirmed.");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server Error during payment verification: " + e.getMessage());
        }
    }


    private String generateRazorpaySignature(String data, String secret) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Convert to HEX (not Base64)
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }


    @Transactional
    public ResponseEntity<?> markOrderAsShipped(Integer orderId, Integer sellerId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Order not found.");
        }

        Order order = orderOpt.get();

        // ✅ Ensure only the actual seller of the product can update it
        if (!order.getProduct().getSeller().getId().equals(sellerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized update.");
        }

        if (order.getDeliveryStatus() != DeliveryStatus.PLACED) {
            return ResponseEntity.badRequest().body("Only placed orders can be marked as shipped.");
        }

        order.setDeliveryStatus(DeliveryStatus.SHIPPED);
        orderRepository.save(order);

        // ✅ Optional WebSocket notification
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ORDER_STATUS_UPDATED");
        payload.put("orderId", orderId);
        payload.put("newStatus", "SHIPPED");
        payload.put("productId", order.getProduct().getId());
        payload.put("sellerId", sellerId);

        notificationService.notifyTopic("orders", payload);

        return ResponseEntity.ok("Order marked as shipped.");
    }


    @Transactional
    public ResponseEntity<?> markOrderAsDeliveredByBuyer(Integer orderId, Integer buyerId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Order not found.");
        }

        Order order = orderOpt.get();

        // Only buyer can confirm delivery
        if (!order.getUser().getId().equals(buyerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized update.");
        }

        if (order.getDeliveryStatus() != DeliveryStatus.SHIPPED) {
            return ResponseEntity.badRequest().body("Only shipped orders can be marked as delivered.");
        }

        order.setDeliveryStatus(DeliveryStatus.DELIVERED);
        order.setStatus(OrderStatus.DELIVERED); // optional if you want to sync order status
        orderRepository.save(order);

        return ResponseEntity.ok("Order marked as delivered.");
    }

    
    private ResponseEntity<?> initiateRefund(Payment payment, String reason) {
        try {
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("payment_id", payment.getRazorpayPaymentId());
            refundRequest.put("notes", Map.of("reason", reason));

            Refund refund = razorpayClient.payments.refund(refundRequest);

            payment.setRefundInitiatedAt(LocalDateTime.now());
            payment.setRefundReason(reason + " by user");
            payment.setRazorpayRefundId(refund.get("id"));
            payment.setPaymentStatus(PaymentStatus.REFUND_INITIATED);
            paymentRepository.save(payment);

            RefundResponseDTO response = new RefundResponseDTO();
            response.setMessage("Order returned and refund initiated.");
            response.setRefundInitiated(true);
            response.setRazorpayRefundId(refund.get("id"));

            return ResponseEntity.ok(response);

        } catch (RazorpayException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Refund failed: " + e.getMessage());
        }
    }

    
    @Transactional
    public ResponseEntity<?> cancelOrder(Integer orderId, Integer userId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Order not found.");
        }

        Order order = orderOpt.get();

        if (!order.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized cancellation attempt.");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            return ResponseEntity.badRequest().body("Only pending or confirmed orders can be cancelled.");
        }

        Payment payment = paymentRepository.findByOrder(order).orElse(null);
        boolean paidOnline = payment != null && payment.getPaymentStatus() == PaymentStatus.SUCCESS;

        // Update order status
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelCompletedAt(LocalDateTime.now());
        orderRepository.save(order);

        if (paidOnline) {
            payment.setPaymentStatus(PaymentStatus.REFUND_INITIATED);
            payment.setRefundInitiatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            initiateRefund(payment, "Order Cancellation");
        }

        return ResponseEntity.ok("Order cancelled" + (paidOnline ? " and refund initiated." : "."));
    }

    
    public ResponseEntity<?> updatePaymentStatusAfterRefund(Integer paymentId) {
        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Payment not found.");
        }

        Payment payment = paymentOpt.get();

        if (payment.getPaymentStatus() != PaymentStatus.REFUND_INITIATED) {
            return ResponseEntity.badRequest().body("Refund not initiated or already processed.");
        }

        // Optionally verify refund type (cancellation) via additional field or log

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        payment.setRefundCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return ResponseEntity.ok("Payment status updated to REFUNDED (Cancellation).");
    }

    
    @Transactional
    public ResponseEntity<?> returnOrder(Integer orderId, Integer userId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Order not found.");
        }

        Order order = orderOpt.get();

        if (!order.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized return attempt.");
        }

        if (order.getStatus() == OrderStatus.RETURNED) {
            return ResponseEntity.badRequest().body("Order already marked as returned.");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            return ResponseEntity.badRequest().body("Only delivered orders can be returned.");
        }

        order.setStatus(OrderStatus.RETURNED);
        order.setReturnCompletedAt(LocalDateTime.now());
        orderRepository.save(order);

        Payment payment = paymentRepository.findByOrder(order).orElse(null);
        if (payment != null && payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            payment.setPaymentStatus(PaymentStatus.REFUND_INITIATED);
            payment.setRefundInitiatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            initiateRefund(payment, "Order Return");
        }

        return ResponseEntity.ok("Order returned and refund initiated.");
    }

    
    public ResponseEntity<?> updateReturnRefundStatus(Integer paymentId) {
        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Payment not found.");
        }

        Payment payment = paymentOpt.get();

        if (payment.getPaymentStatus() != PaymentStatus.REFUND_INITIATED) {
            return ResponseEntity.badRequest().body("Refund not initiated or already processed.");
        }

        // Optionally verify refund type (return) via additional field or log

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        payment.setRefundCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return ResponseEntity.ok("Return refund status updated to REFUNDED.");
    }




    @Transactional
    public ResponseEntity<?> replaceOrder(Integer orderId, Integer userId, String reason) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Order not found.");
        }

        Order order = orderOpt.get();

        // Verify user
        if (!order.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized replacement attempt.");
        }

        // Only allow replacement if order was delivered
        if (order.getStatus() != OrderStatus.DELIVERED) {
            return ResponseEntity.badRequest().body("Only delivered orders can be replaced.");
        }

        // Prevent double replacement
        if (order.getReplacementStatus() == RequestStatus.REQUESTED || 
            order.getReplacementStatus() == RequestStatus.COMPLETED) {
            return ResponseEntity.badRequest().body("Replacement already requested or completed.");
        }

        // Update replacement status
        order.setReplacementStatus(RequestStatus.REQUESTED);
        order.setReplacementReason(reason);
        order.setReplacementRequestedAt(LocalDateTime.now());

        orderRepository.save(order);

        return ResponseEntity.ok("Replacement requested successfully.");
    }

    
    @Transactional
    public ResponseEntity<?> updateReplacementStatus(Integer orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Order not found.");
        }

        Order order = orderOpt.get();

        // Check if replacement was requested
        if (order.getReplacementStatus() != RequestStatus.REQUESTED) {
            return ResponseEntity.badRequest().body("No replacement requested or already fulfilled.");
        }

        // Update replacement status and timestamp
        order.setReplacementStatus(RequestStatus.REPLACED);
        order.setReplacementCompletedAt(LocalDateTime.now());

        orderRepository.save(order);

        return ResponseEntity.ok("Replacement marked as completed.");
    }






//    @Transactional
//    public ResponseEntity<?> markOrderAsDelivered(Integer orderId, Integer userId) {
//        Optional<Order> orderOpt = orderRepository.findById(orderId);
//        if (orderOpt.isEmpty()) {
//            return ResponseEntity.badRequest().body("Order not found.");
//        }
//
//        Order order = orderOpt.get();
//
//        if (!order.getUser().getId().equals(userId)) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized update.");
//        }
//
//        if (order.getStatus() != OrderStatus.CONFIRMED) {
//            return ResponseEntity.badRequest().body("Only confirmed orders can be marked as delivered.");
//        }
//
//        order.setStatus(OrderStatus.DELIVERED);
//        orderRepository.save(order);
//
//        return ResponseEntity.ok("Order marked as delivered.");
//    }

    public ResponseEntity<?> getOrdersByBuyerId(int buyerId) {
        if (buyerId <= 0) {
            return ResponseEntity.badRequest().body("Invalid Buyer ID");
        }

        List<Order> orders = orderRepository.findAllByBuyerIdWithDetails(buyerId);

        if (orders.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No orders found for the buyer");
        }

        List<Map<String, Object>> response = orders.stream().map(order -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("orderId", order.getId());
            map.put("bidId", order.getBid().getId());
            map.put("productId", order.getProduct().getId());
            map.put("productName", order.getProduct().getName());
            map.put("quantity", order.getQuantity());
            map.put("bidAmount", order.getTotalAmount());
            map.put("biddingRound", order.getBid().getBiddingRound());
            map.put("status", order.getStatus());
            map.put("deliveryStatus", order.getDeliveryStatus());
            map.put("orderCreatedAt", order.getCreatedAt());

            List<String> images = productImageRepo.findByProductId(order.getProduct().getId())
                                           .stream()
                                           .map(ProductImage::getImageUrl)
                                           .collect(Collectors.toList());

            map.put("images", images);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getOrdersBySellerId(int sellerId) {
        if (sellerId <= 0) {
            return ResponseEntity.badRequest().body("Invalid Seller ID");
        }

        List<Order> orders = orderRepository.findAllBySellerIdWithDetails(sellerId);

        if (orders.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No orders found for the seller");
        }

        List<Map<String, Object>> response = orders.stream().map(order -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("orderId", order.getId());
            map.put("productId", order.getProduct().getId());
            map.put("productName", order.getProduct().getName());
            map.put("buyerUsername", order.getUser().getUsername());
            map.put("quantity", order.getQuantity());
            map.put("amount", order.getTotalAmount());
            map.put("biddingRound", order.getBid().getBiddingRound());
            map.put("status", order.getStatus());
            map.put("deliveryStatus", order.getDeliveryStatus());
            map.put("orderCreatedAt", order.getCreatedAt());

            List<String> images = productImageRepo.findByProductId(order.getProduct().getId())
                                           .stream()
                                           .map(ProductImage::getImageUrl)
                                           .collect(Collectors.toList());

            map.put("images", images);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
    
    public ResponseEntity<?> getAllOrders() {
        List<Order> orders = orderRepository.findAll(); // Uses default findAll()

        if (orders.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No orders available");
        }

        List<Map<String, Object>> response = orders.stream().map(order -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("orderId", order.getId());
            map.put("buyerUsername", order.getUser().getUsername());
            map.put("productId", order.getProduct().getId());
            map.put("productName", order.getProduct().getName());
            map.put("sellerUsername", order.getProduct().getSeller().getUsername());
            map.put("quantity", order.getQuantity());
            map.put("amount", order.getTotalAmount());
            map.put("biddingRound", order.getBid().getBiddingRound());
            map.put("status", order.getStatus());
            map.put("deliveryStatus", order.getDeliveryStatus());
            map.put("orderCreatedAt", order.getCreatedAt());

            List<String> images = productImageRepo.findByProductId(order.getProduct().getId())
                                           .stream()
                                           .map(ProductImage::getImageUrl)
                                           .collect(Collectors.toList());
            map.put("images", images);

            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

}