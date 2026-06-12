package com.example.flexbid.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flexbid.dto.ServiceRequest;
import com.example.flexbid.model.Buyer;
import com.example.flexbid.model.User;
import com.example.flexbid.repository.ServiceBuyerRepository;
import com.example.flexbid.repository.UserRepository;

@Service
public class BuyerService {
	
	@Autowired
    private ServiceBuyerRepository serviceBuyerRepo;
	
	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private WebSocketNotificationService notificationService;

	
    /**
     * Create a service request by buyer with manual validations.
     */
	
	public ResponseEntity<?> createServiceRequest( ServiceRequest request,
	                                               int buyerId) {

	    // 🔐 Basic null check
	    if (request == null) {
	        return ResponseEntity.badRequest().body(Map.of("message", "Request body is missing."));
	    }

	    // 📋 Field Validations
	    if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
	        return ResponseEntity.badRequest().body(Map.of("message", "Title is required."));
	    }

	    if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
	        return ResponseEntity.badRequest().body(Map.of("message", "Description is required."));
	    }

	    if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
	        return ResponseEntity.badRequest().body(Map.of("message", "Category is required."));
	    }

	    if (request.getMaxBudget() == null) {
	        return ResponseEntity.badRequest().body(Map.of("message", "Maximum budget is required."));
	    }

	    if (request.getMaxBudget().compareTo(BigDecimal.ONE) < 0) {
	        return ResponseEntity.badRequest().body(Map.of("message", "Maximum budget must be at least ₹1."));
	    }

//	    if (request.getDurationDays() < 1) {
//	        return ResponseEntity.badRequest().body(Map.of("message", "Service duration must be at least 1 day."));
//	    }

	    if (request.getScheduleDate() == null || request.getScheduleDate().isBefore(LocalDate.now())) {
	        return ResponseEntity.badRequest().body(Map.of("message", "Schedule date must be today or later."));
	    }

	    if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
	        return ResponseEntity.badRequest().body(Map.of("message", "Location is required."));
	    }

	    // 🕒 Bidding window: Start = now + 1 min, End = N days later at 6 PM
//	    LocalDateTime biddingStart = LocalDateTime.now().plusMinutes(1);
//	    LocalDateTime biddingEnd = biddingStart.plusDays(request.getDurationDays());
//	    biddingEnd = biddingEnd.withHour(18).withMinute(0).withSecond(0).withNano(0); // Set to 6 PM

	    LocalDateTime biddingStart = LocalDateTime.now().plusMinutes(1); // Immediate bidding
	    LocalDateTime biddingEnd = biddingStart
	        .plusDays(request.getDurationDays())
	        .plusHours(request.getDurationHours())
	        .plusMinutes(request.getDurationMinutes());
	    
	    // 🏗️ Create Buyer entity
	    Buyer buyer = new Buyer();
	    buyer.setBuyerId(buyerId);
	    buyer.setTitle(request.getTitle().trim());
	    buyer.setDescription(request.getDescription().trim());
	    buyer.setCategory(request.getCategory().trim());
	    buyer.setMaxBudget(request.getMaxBudget());
	    buyer.setBiddingStart(biddingStart);
	    buyer.setBiddingEnd(biddingEnd);
	    buyer.setStatus("OPEN");
	    buyer.setCreatedAt(LocalDateTime.now());
	    buyer.setScheduleDate(request.getScheduleDate());
	    buyer.setLocation(request.getLocation().trim());
	    buyer.setScheduleConfirmed(false);
	    buyer.setRescheduleCount(0);

	    // 💾 Save to DB
	    Buyer saved = serviceBuyerRepo.save(buyer);

	    // 📡 WebSocket broadcast
	    Map<String, Object> notification = Map.of(
	        "type", "NEW_SERVICE_REQUEST",
	        "requestId", saved.getId(),
	        "title", saved.getTitle(),
	        "category", saved.getCategory(),
	        "budget", saved.getMaxBudget(),
	        "scheduleDate", saved.getScheduleDate(),
	        "biddingStart", saved.getBiddingStart(),
	        "biddingEnd", saved.getBiddingEnd()
	    );

	    notificationService.broadcastToTopic("/topic/service-requests", notification);

	    // ✅ Return response
	    Map<String, Object> response = Map.of(
	        "message", "Service request created successfully.",
	        "requestId", saved.getId(),
	        "biddingStart", saved.getBiddingStart(),
	        "biddingEnd", saved.getBiddingEnd(),
	        "status", saved.getStatus(),
	        "scheduleDate", saved.getScheduleDate()
	    );

	    return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}


    /**
     * Get all requests by a specific buyer.
     */
	public ResponseEntity<Map<String, Object>> getServiceRequestHistoryByBuyer(int buyerId) {
	    List<Buyer> allRequests = serviceBuyerRepo.findByBuyerId(buyerId);

	    if (allRequests.isEmpty()) {
	        Map<String, Object> notFoundResponse = new HashMap<>();
	        notFoundResponse.put("status", "error");
	        notFoundResponse.put("message", "No service requests found for buyer with ID: " + buyerId);
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFoundResponse);
	    }

	    // Categorize by status
	    List<Buyer> openRequests = allRequests.stream()
	            .filter(req -> "OPEN".equalsIgnoreCase(req.getStatus()))
	            .toList();

	    List<Buyer> scheduledRequests = allRequests.stream()
	            .filter(req -> "SCHEDULED".equalsIgnoreCase(req.getStatus()))
	            .toList();

	    List<Buyer> closedRequests = allRequests.stream()
	            .filter(req -> "CLOSED".equalsIgnoreCase(req.getStatus()))
	            .toList();

	    List<Buyer> expiredRequests = allRequests.stream()
	            .filter(req -> "EXPIRED".equalsIgnoreCase(req.getStatus()))
	            .toList();

	    List<Buyer> rescheduledRequests = allRequests.stream()
	            .filter(req -> "RESCHEDULED".equalsIgnoreCase(req.getStatus()))
	            .toList();
	    List<Buyer> completedRequests = allRequests.stream()
	            .filter(req -> "COMPLETED".equalsIgnoreCase(req.getStatus()))
	            .toList();
	    // Prepare JSON response
	    Map<String, Object> response = new LinkedHashMap<>();
	    response.put("status", "success");
	    response.put("buyerId", buyerId);
	    response.put("openRequests", openRequests);
	    response.put("scheduledRequests", scheduledRequests);
	    response.put("closedRequests", closedRequests);
	    response.put("expiredRequests", expiredRequests);
	    response.put("rescheduledRequests", rescheduledRequests);
	    response.put("completedRequests", completedRequests);
	    response.put("message", "Your service request history fetched successfully.");

	    // Optional WebSocket Notification
	    Map<String, Object> notification = new HashMap<>();
	    notification.put("type", "HISTORY_FETCHED");
	    notification.put("buyerId", buyerId);
	    notification.put("totalRequests", allRequests.size());
	    notification.put("timestamp", LocalDateTime.now());

	    notificationService.sendToUser("buyer-" + buyerId, notification);

	    return ResponseEntity.ok(response);
	}


    
	public ResponseEntity<?> getAllOpenServiceRequestsForSellers() {
	    List<Buyer> openRequests = serviceBuyerRepo.findAll().stream()
	            .filter(req -> "OPEN".equalsIgnoreCase(req.getStatus()))
	            .toList();

	    Map<String, Object> response = new LinkedHashMap<>();

	    if (openRequests.isEmpty()) {
	        response.put("openRequests", List.of());
	        response.put("totalOpenRequests", 0);
	        response.put("message", "No open service requests are currently available for bidding.");
	        return ResponseEntity.ok(response);
	    }

	    // Prepare trimmed down DTO-like response (recommended)
	    List<Map<String, Object>> simplifiedRequests = openRequests.stream().map(req -> {
	        Map<String, Object> map = new HashMap<>();
	        map.put("id", req.getId());
	        map.put("buyerId", req.getBuyerId());
	        map.put("title", req.getTitle());
	        map.put("description", req.getDescription());
	        map.put("category", req.getCategory());
	        map.put("maxBudget", req.getMaxBudget());
	        map.put("biddingStart", req.getBiddingStart());
	        map.put("biddingEnd", req.getBiddingEnd());
	        map.put("scheduleDate", req.getScheduleDate());
	        map.put("location", req.getLocation());
	        map.put("reschedulePending", req.isReschedulePending());
	        return map;
	    }).toList();

	    response.put("openRequests", simplifiedRequests);
	    response.put("totalOpenRequests", simplifiedRequests.size());
	    response.put("message", "Open service requests available for seller bidding.");

	    // 🔔 WebSocket Broadcast
	    notificationService.broadcastToTopic("/topic/service-requests", simplifiedRequests);

	    return ResponseEntity.ok(response);
	}


    
	public ResponseEntity<?> getRequestById(int requestId) {
	    Optional<Buyer> requestOpt = serviceBuyerRepo.findById(requestId);

	    if (requestOpt.isEmpty()) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                .body(Map.of("message", "Service request not found."));
	    }

	    Buyer request = requestOpt.get();

	    // Use a response DTO or Map instead of returning the full entity
	    Map<String, Object> response = new LinkedHashMap<>();
	    response.put("requestId", request.getId());
	    response.put("title", request.getTitle());
	    response.put("description", request.getDescription());
	    response.put("category", request.getCategory());
	    response.put("maxBudget", request.getMaxBudget());
	    response.put("status", request.getStatus());
	    response.put("createdAt", request.getCreatedAt());
	    response.put("biddingEnd", request.getBiddingEnd());
	    response.put("scheduleDate", request.getScheduleDate());
	    response.put("scheduleConfirmed", request.isScheduleConfirmed());
	    response.put("rescheduleCount", request.getRescheduleCount());
	    response.put("winnerId", request.getWinnerId());

	    return ResponseEntity.ok(response);
	}



    public ResponseEntity<?> getBuyerStats(int buyerId) {
        List<Buyer> all = serviceBuyerRepo.findByBuyerId(buyerId);

        if (all.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No requests found for this buyer."));
        }

        Map<String, Long> stats = all.stream()
                .collect(Collectors.groupingBy(Buyer::getStatus, Collectors.counting()));

        Optional<LocalDateTime> latestUpdate = all.stream()
                .map(Buyer::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalRequests", all.size());
        response.put("statusStats", stats);
        response.put("lastUpdatedAt", latestUpdate.orElse(null));

        return ResponseEntity.ok(response);
    }

    
    
    @Transactional
    public ResponseEntity<?> requestRescheduleDate(int requestId, LocalDate rescheduleDate) {
        Optional<Buyer> opt = serviceBuyerRepo.findById(requestId);
        if (opt.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Request not found");

        Buyer request = opt.get();

        Integer winnerId = request.getWinnerId();
        if (winnerId == null)
            return ResponseEntity.badRequest().body("This request has no assigned seller yet");

        Optional<User> userOpt = userRepo.findById(winnerId);
        if (userOpt.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Winner user not found");

        // Validate current schedule
        if (request.getScheduleDate() == null)
            return ResponseEntity.badRequest().body("Service not scheduled yet");

        if (rescheduleDate == null || !rescheduleDate.isAfter(LocalDate.now()))
            return ResponseEntity.badRequest().body("Invalid reschedule date");

        if (rescheduleDate.equals(request.getScheduleDate()))
            return ResponseEntity.badRequest().body("New date must differ from current schedule");

        // Set new reschedule info
        request.setRescheduleDate(rescheduleDate);
        request.setReschedulePending(true);
        request.setRescheduleAccepted(false); // Ensure it's unset
        request.setScheduleConfirmed(false); // Until accepted
        serviceBuyerRepo.save(request);

        // Prepare WebSocket payload for Buyer
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "RESCHEDULE_REQUESTED");
        payload.put("requestId", requestId);
        payload.put("title", request.getTitle());
        payload.put("category", request.getCategory());
        payload.put("location", request.getLocation());
        payload.put("originalScheduleDate", request.getScheduleDate().toString());
        payload.put("rescheduleDate", rescheduleDate.toString());
        payload.put("sellerId", winnerId);
        payload.put("status", request.getStatus());

        String buyerEmail = userOpt.get().getEmail();
        if (buyerEmail != null) {
            notificationService.sendToUser(buyerEmail, payload); // 🔐 Notify Buyer privately
        }

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Reschedule request submitted for buyer approval",
            "rescheduleDate", rescheduleDate,
            "requestId", requestId
        ));
    }


    @Transactional
    public ResponseEntity<?> respondToRescheduleDate(int requestId, boolean accepted) {
        Optional<Buyer> opt = serviceBuyerRepo.findById(requestId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Request not found");
        }

        Buyer request = opt.get();

        if (!request.isReschedulePending()) {
            return ResponseEntity.badRequest().body("No reschedule request pending for this service");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "RESCHEDULE_RESPONSE");
        payload.put("requestId", requestId);
        payload.put("title", request.getTitle());
        payload.put("category", request.getCategory());
        payload.put("location", request.getLocation());
        payload.put("previousScheduleDate", request.getScheduleDate().toString());
        payload.put("requestedRescheduleDate", request.getRescheduleDate().toString());
        payload.put("accepted", accepted);

        String sellerUserId = "seller-" + request.getWinnerId(); // STOMP topic for seller

        String message;
        if (accepted) {
            request.setScheduleDate(request.getRescheduleDate());
            request.setRescheduleAccepted(true);
            request.setScheduleConfirmed(true);
            request.setRescheduleCount(request.getRescheduleCount() + 1);
            request.setStatus("RESCHEDULED");

            payload.put("newScheduleDate", request.getScheduleDate().toString());
            payload.put("status", "RESCHEDULED");

            message = "Reschedule approved. Schedule updated.";
        } else {
            request.setRescheduleAccepted(false);
            request.setStatus("EXPIRED");

            payload.put("status", "EXPIRED");
            message = "Reschedule declined. Request marked as expired.";
        }

        request.setReschedulePending(false);
        request.setRescheduleDate(null); // Clear proposed date
        serviceBuyerRepo.save(request);

        notificationService.sendToUser(sellerUserId, payload); // Notify seller

        // ✅ Return response safely
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("message", message);
        response.put("requestId", requestId);
        response.put("rescheduleAccepted", accepted);
        if (accepted) {
            response.put("newScheduleDate", request.getScheduleDate());
        }
        response.put("serviceStatus", request.getStatus());

        return ResponseEntity.ok(response);
    }

}

