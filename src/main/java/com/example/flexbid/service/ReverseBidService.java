package com.example.flexbid.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flexbid.model.Buyer;
import com.example.flexbid.model.ReverseBid;
//import com.example.flexbid.model.SellerPortfolioItem;
import com.example.flexbid.model.SellerProfile;
import com.example.flexbid.model.User;
import com.example.flexbid.repository.ReverseBidRepository;
//import com.example.flexbid.repository.SellerPortfolioItemRepository;
import com.example.flexbid.repository.SellerProfileRepository;
import com.example.flexbid.repository.ServiceBuyerRepository;
import com.example.flexbid.repository.UserRepository;

@Service
public class ReverseBidService {

	@Autowired
    private ReverseBidRepository reverseBidRepo;

    @Autowired
    private ServiceBuyerRepository serviceBuyerRepo;
    
    @Autowired
    private UserRepository userRepo;
    
    @Autowired
    private SellerProfileRepository sellerProfileRepo;

    @Autowired
	private WebSocketNotificationService notificationService;

    @Transactional
    public ResponseEntity<?> placeReverseBid(int requestId, int sellerId, BigDecimal bidAmount) {
        Optional<User> optionalUser = userRepo.findById(sellerId);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().body("Seller not found");
        }

        Optional<SellerProfile> profile = sellerProfileRepo.findByUserId(sellerId);
        if (profile.isEmpty()) {
            return ResponseEntity.badRequest().body("Add portfolio to participate in bidding");
        }

        if (bidAmount == null || bidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("Bid amount must be greater than 0.");
        }

        Optional<Buyer> requestOpt = serviceBuyerRepo.findById(requestId);
        if (requestOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Service request not found.");
        }

        Buyer request = requestOpt.get();
        if (!"OPEN".equalsIgnoreCase(request.getStatus())) {
            return ResponseEntity.badRequest().body("This service request is not open for bidding.");
        }


        // Check if bid is lower than the current lowest bid
        ReverseBid lowestBid = reverseBidRepo.findTopByRequestIdOrderByBidAmountAsc(requestId);
        if (lowestBid != null && bidAmount.compareTo(lowestBid.getBidAmount()) >= 0) {
            return ResponseEntity.badRequest()
                    .body("Your bid must be lower than the current lowest bid of ₹" + lowestBid.getBidAmount());
        }

        // Check if same seller already placed same amount
        ReverseBid lastBidBySeller = reverseBidRepo.findTopByRequestIdAndSellerIdOrderByCreatedAtDesc(requestId, sellerId);
        if (lastBidBySeller != null && bidAmount.compareTo(lastBidBySeller.getBidAmount()) == 0) {
            return ResponseEntity.badRequest()
                    .body("You already placed this bid amount. Try a lower bid.");
        }

        if (bidAmount.compareTo(request.getMaxBudget()) > 0) {
            return ResponseEntity.badRequest()
                    .body("Your bid exceeds the maximum budget of ₹" + request.getMaxBudget());
        }

        // Prevent consecutive bids by same seller
        ReverseBid lastBid = reverseBidRepo.findTopByRequestIdOrderByCreatedAtDesc(requestId);
        if (lastBid != null && Objects.equals(lastBid.getSellerId(), sellerId)) {
            return ResponseEntity.badRequest()
                    .body("Wait for another seller to place a bid before bidding again.");
        }

        // Save new bid
        ReverseBid newBid = new ReverseBid();
        newBid.setRequestId(requestId);
        newBid.setSellerId(sellerId);
        newBid.setBidAmount(bidAmount);
        newBid.setCreatedAt(LocalDateTime.now());
        reverseBidRepo.save(newBid);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "NEW_REVERSE_BID");
        payload.put("requestId", requestId);
        payload.put("sellerId", sellerId);
        payload.put("sellerName",optionalUser.get().getUsername());
        payload.put("bidAmount", bidAmount);
        payload.put("timestamp", newBid.getCreatedAt());

        // Broadcast
        notificationService.notifyTopic("reverse-bids/" + requestId, payload);

        return ResponseEntity.status(HttpStatus.CREATED).body("Bid placed successfully.");
    }



    
    @Transactional
    @Scheduled(cron = "0 */5 * * * *") // every 5 mins
    public void finalizeExpiredReverseBids() {
        List<Buyer> expired = serviceBuyerRepo.findAll().stream()
                .filter(req -> "OPEN".equalsIgnoreCase(req.getStatus()) &&
                               req.getBiddingEnd().isBefore(LocalDateTime.now()))
                .collect(Collectors.toList());

        for (Buyer request : expired) {
            List<ReverseBid> bids = reverseBidRepo.findByRequestId(request.getId());

            if (!bids.isEmpty()) {
                ReverseBid winningBid = bids.stream()
                        .min(Comparator.comparing(ReverseBid::getBidAmount))
                        .orElse(null);

                if (winningBid != null) {
                    winningBid.setWinner(true);
                    reverseBidRepo.save(winningBid);
                    request.setStatus("SCHEDULED");
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("type", "REVERSE_BID_FINALIZED");
                    payload.put("status", "SCHEDULED");
                    payload.put("requestId", request.getId());
                    payload.put("winnerSellerId", winningBid.getSellerId());
                    payload.put("bidAmount", winningBid.getBidAmount());
                    payload.put("timestamp", LocalDateTime.now());

                    // Notify all watching this request
                    notificationService.notifyTopic("reverse-bids/" + request.getId(), payload);

                    // Optionally notify winner privately
                    notificationService.sendToUser("seller-" + winningBid.getSellerId(), payload);
                }
            } else {
                request.setStatus("CLOSED");

                // 🔔 WebSocket: Notify request closed due to no bids
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "REVERSE_BID_CLOSED");
                payload.put("status", "CLOSED");
                payload.put("requestId", request.getId());
                payload.put("timestamp", LocalDateTime.now());

                notificationService.notifyTopic("reverse-bids/" + request.getId(), payload);
            }           
            
            serviceBuyerRepo.save(request);
        }
    }

    /**
     * View winner details for expired service requests.
     */
    public ResponseEntity<?> getFinalizedResults() {
        List<Buyer> processed = serviceBuyerRepo.findAll().stream()
                .filter(req -> !"OPEN".equalsIgnoreCase(req.getStatus()) &&
                               req.getBiddingEnd().isBefore(LocalDateTime.now()))
                .collect(Collectors.toList());

        List<Map<String, Object>> results = new ArrayList<>();

        for (Buyer request : processed) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("requestId", request.getId());
            map.put("status", request.getStatus());

            List<ReverseBid> bids = reverseBidRepo.findByRequestId(request.getId());
            Optional<ReverseBid> winner = bids.stream().filter(ReverseBid::isWinner).findFirst();

            if (winner.isPresent()) {
                map.put("winnerSellerId", winner.get().getSellerId());
                map.put("winningBid", winner.get().getBidAmount());
            } else {
                map.put("message", "No winning bid or no bids placed");
            }

            results.add(map);
        }

        return ResponseEntity.ok(results);
    }

    @Transactional
    @Scheduled(fixedRate = 60000)
    public void finalizeServiceRequests() {
        LocalDateTime now = LocalDateTime.now();

        // Step 1: Get all expired requests with OPEN status
        List<Buyer> expiredRequests = serviceBuyerRepo.findByStatusAndBiddingEndBefore("OPEN", now);

        for (Buyer request : expiredRequests) {
            try {
                // Step 2: Get all bids for the request (ascending order -> lowest bid first)
                List<ReverseBid> bids = reverseBidRepo.findByRequestIdOrderByBidAmountAsc(request.getId());

                if (!bids.isEmpty()) {
                    ReverseBid winningBid = bids.get(0); // 🏆 Lowest bid wins

                    // Step 3: Mark request as awarded
                    request.setStatus("SCHEDULED");
                    request.setWinnerId(winningBid.getSellerId());
                    request.setScheduleDate(null);
                    request.setUpdatedAt(now);

                    // ✅ Auto-assign schedule date if missing
                    if (request.getScheduleDate() == null) {
                        request.setScheduleDate(LocalDate.now().plusDays(1));
                    }

                    request.setRescheduleCount(0);
                    request.setScheduleConfirmed(false);

                    serviceBuyerRepo.save(request);

                    // Step 4: Mark bid as winner
                    winningBid.setWinner(true);
                    reverseBidRepo.save(winningBid);

                    System.out.println("✅ Winner finalized: Seller ID " + winningBid.getSellerId() +
                            " for Request ID: " + request.getId() +
                            ", Bid Amount: " + winningBid.getBidAmount());
                } else {
                    // No bids: mark as expired
                    request.setStatus("EXPIRED");
                    request.setUpdatedAt(now);
                    serviceBuyerRepo.save(request);

                    System.out.println("⚠️ Request expired (no bids): Request ID " + request.getId());
                }
            } catch (Exception e) {
                System.err.println("❌ Error finalizing request ID " + request.getId() + ": " + e.getMessage());
            }
        }
    }

    
    @Transactional
    public ResponseEntity<?> manuallyFinalize(int requestId) {
        Optional<Buyer> opt = serviceBuyerRepo.findById(requestId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Service request not found.");
        }

        Buyer request = opt.get();
        String status = request.getStatus();
        LocalDateTime now = LocalDateTime.now();

        // 🛑 Already finalized
        if ("SCHEDULED".equalsIgnoreCase(status)) {
            Optional<ReverseBid> winnerOpt = reverseBidRepo.findFirstByRequestIdAndIsWinnerTrue(requestId);
            if (winnerOpt.isPresent()) {
                ReverseBid winner = winnerOpt.get();
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("message", "Winner already finalized.");
                response.put("requestId", request.getId());
                response.put("status", status);
                response.put("winnerId", winner.getSellerId());
                response.put("winningBid", winner.getBidAmount());
                response.put("finalizedAt", request.getUpdatedAt());
                response.put("scheduleDate", request.getScheduleDate());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                     .body("Winner marked as finalized but no matching bid found.");
            }
        }

        if ("CLOSED".equalsIgnoreCase(status)) {
            return ResponseEntity.badRequest().body("Service request is already closed.");
        }

        if ("EXPIRED".equalsIgnoreCase(status)) {
            return ResponseEntity.badRequest().body("Service request has expired due to no bids.");
        }

        if ("OPEN".equalsIgnoreCase(status)) {
            boolean biddingEnded = now.isAfter(request.getBiddingEnd());

            List<ReverseBid> bids = reverseBidRepo.findByRequestIdOrderByBidAmountAsc(requestId);
            if (bids.isEmpty()) {
                if (biddingEnded) {
                    request.setStatus("EXPIRED");
                    request.setUpdatedAt(now);
                    serviceBuyerRepo.save(request);
                    return ResponseEntity.ok("No bids received. Request has expired.");
                } else {
                    return ResponseEntity.badRequest().body("Bidding is still open and no bids received yet.");
                }
            }

            ReverseBid winningBid = bids.get(0);
            request.setStatus("SCHEDULED");
            request.setWinnerId(winningBid.getSellerId());
            request.setUpdatedAt(now);

            // ✅ Auto-assign schedule date if missing
            if (request.getScheduleDate() == null) {
                request.setScheduleDate(LocalDate.now().plusDays(1));
            }

            request.setRescheduleCount(0);
            request.setScheduleConfirmed(false);

            serviceBuyerRepo.save(request);

            winningBid.setWinner(true);
            reverseBidRepo.save(winningBid);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "Winner finalized successfully.");
            response.put("requestId", request.getId());
            response.put("status", request.getStatus());
            response.put("winnerId", winningBid.getSellerId());
            response.put("winningBid", winningBid.getBidAmount());
            response.put("finalizedAt", request.getUpdatedAt());
            response.put("scheduleDate", request.getScheduleDate());
            
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "REVERSE_BID_FINALIZED");
            payload.put("message", "Winner finalized successfully.");
            payload.put("requestId", request.getId());
            payload.put("status", request.getStatus());
            payload.put("winnerId", winningBid.getSellerId());
            payload.put("winningBid", winningBid.getBidAmount());
            payload.put("finalizedAt", request.getUpdatedAt());
            payload.put("scheduleDate", request.getScheduleDate());

            // 🌐 Public topic
            notificationService.notifyTopic("reverse-bids/" + request.getId(), payload);

            // 📩 Private message to winner
            notificationService.sendToUser("seller-" + winningBid.getSellerId(), payload);

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body("Unsupported service request status: " + status);
    }
    
   
    
    public ResponseEntity<?> getBidsForRequest(int requestId) {
        List<ReverseBid> bids = reverseBidRepo.findByRequestIdOrderByCreatedAtDesc(requestId);

        // Step 1: Batch fetch sellers
        List<Integer> sellerIds = bids.stream()
            .map(ReverseBid::getSellerId)
            .distinct()
            .collect(Collectors.toList());

        Map<Integer, String> sellerIdToName = userRepo.findAllById(sellerIds).stream()
            .collect(Collectors.toMap(User::getId, User::getUsername));

        // Step 2: Build response
        List<Map<String, Object>> response = bids.stream().map(bid -> {
            Map<String, Object> bidMap = new HashMap<>();
            bidMap.put("id", bid.getId()); // 👈 important for frontend key
            bidMap.put("bidAmount", bid.getBidAmount());
            bidMap.put("sellerId", bid.getSellerId());
            bidMap.put("timestamp", bid.getCreatedAt());
            bidMap.put("sellerName", sellerIdToName.getOrDefault(bid.getSellerId(), "Unknown"));
            return bidMap;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }


//    @GetMapping("/winner/seller/{sellerId}")
    public ResponseEntity<?> getWinningRequestsForSeller( int sellerId) {
        List<ReverseBid> winningBids = reverseBidRepo.findBySellerIdAndIsWinnerTrue(sellerId);

        if (winningBids.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Map<String, Object>> responseList = new ArrayList<>();

        for (ReverseBid bid : winningBids) {
            Optional<Buyer> requestOpt = serviceBuyerRepo.findById(bid.getRequestId());

            if (requestOpt.isPresent()) {
                Buyer request = requestOpt.get();

                Map<String, Object> map = new HashMap<>();
                map.put("requestId", request.getId());
                map.put("title", request.getTitle());
                map.put("category", request.getCategory());
                map.put("description", request.getDescription());
                map.put("status", request.getStatus());
                map.put("maxBudget", request.getMaxBudget());
                map.put("location", request.getLocation());
                map.put("scheduleDate", request.getScheduleDate());
                map.put("rescheduleDate", request.getRescheduleDate());

                map.put("bidAmount", bid.getBidAmount());
                map.put("timestamp", bid.getCreatedAt());

                responseList.add(map);
            }
        }

        return ResponseEntity.ok(responseList);
    }

    @Transactional
    public ResponseEntity<?> markServiceAsCompleted(int requestId, int buyerId) {
        Optional<Buyer> opt = serviceBuyerRepo.findById(requestId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Service request not found.");
        }

        Buyer request = opt.get();

        // ✅ Confirm only the actual buyer can complete the service
        if (request.getBuyerId() != buyerId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body("Only the buyer can mark this service as completed.");
        }

        // ✅ Must be in either SCHEDULED or RESCHEDULED state
        String status = request.getStatus();
        if (!"SCHEDULED".equalsIgnoreCase(status) && !"RESCHEDULED".equalsIgnoreCase(status)) {
            return ResponseEntity.badRequest().body("Only scheduled or rescheduled services can be marked as completed.");
        }

        // ⏳ Determine the effective service date
        LocalDate effectiveDate = request.isRescheduleAccepted() ? request.getScheduleDate() : request.getScheduleDate();
        if (effectiveDate == null) {
            return ResponseEntity.badRequest().body("Service date is not set.");
        }

        // ⏱️ Ensure today's date is on or after the final service date
        LocalDate today = LocalDate.now();
        if (today.isBefore(effectiveDate)) {
            return ResponseEntity.badRequest().body("Cannot mark as completed before the scheduled/rescheduled date.");
        }

        // ✅ Mark as COMPLETED
        request.setStatus("COMPLETED");
        request.setUpdatedAt(LocalDateTime.now());
        serviceBuyerRepo.save(request);

        // 🔔 Prepare WebSocket notification payload
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "SERVICE_COMPLETED");
        payload.put("requestId", request.getId());
        payload.put("status", "COMPLETED");
        payload.put("completedAt", request.getUpdatedAt());

        // 🌐 Public notification
        notificationService.notifyTopic("reverse-bids/" + request.getId(), payload);

        // 🔐 Private notification to the seller
        if (request.getWinnerId() != null) {
            notificationService.sendToUser("seller-" + request.getWinnerId(), payload);
        }

        return ResponseEntity.ok("Service marked as completed.");
    }


}
