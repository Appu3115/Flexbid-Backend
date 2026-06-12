package com.example.flexbid.service;

import java.math.BigDecimal;
import java.time.Duration;
//import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flexbid.dto.BidResponseDTO;
import com.example.flexbid.dto.Winner;
import com.example.flexbid.model.Bid;
import com.example.flexbid.model.BiddingTimeSlot;
import com.example.flexbid.model.DailyBiddingTimeConfig;
import com.example.flexbid.model.Product;
import com.example.flexbid.model.ProductImage;
import com.example.flexbid.model.ProductStatus;
import com.example.flexbid.model.User;
import com.example.flexbid.repository.BidRepository;
import com.example.flexbid.repository.BiddingTimeSlotRepository;
import com.example.flexbid.repository.DailyBiddingTimeConfigRepository;
import com.example.flexbid.repository.ProductImageRepository;
import com.example.flexbid.repository.ProductRepository;
import com.example.flexbid.repository.UserRepository;



@Service
public class BidService {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DailyBiddingTimeConfigRepository dailyConfigRepository;

    @Autowired
    private BiddingTimeSlotRepository slotRepository;
    
    @Autowired
    private ProductImageRepository productImageRepository;
    
    @Autowired
    private AuctionTimerService auctionTimerService;
    
    @Autowired
    private WebSocketNotificationService notificationService;
    
    @Autowired
    private EmailService emailService;


    // Start bidding for a product
    @Transactional
    public ResponseEntity<?> placeBid(int buyerId, int productId, BigDecimal bidAmount, boolean isIncrement) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found.");
        }

        Optional<User> userOpt = userRepository.findById(buyerId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found.");
        }

        User user = userOpt.get();
        Product product = productOpt.get();
        
        if(product.getSeller().getId()==buyerId) {
        	return ResponseEntity.badRequest().body("You cannot bid on your own product");
        }

        if (product.getProductStatus() != ProductStatus.ACTIVE || product.isFinalized()) {
            return ResponseEntity.badRequest().body("Product is not available for bidding.");
        }

        if (product.getStock() <= 0) {
            return ResponseEntity.badRequest().body("Product out of stock.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalTime nowTime = now.toLocalTime();
        LocalDate today = now.toLocalDate();

        boolean inAllowedWindow = false;
        boolean inFinal3Minutes = false;

        DailyBiddingTimeConfig config = dailyConfigRepository.findById(1).orElse(null);
        if (config != null && isWithinTimeWindow(nowTime, config.getDailyStartTime(), config.getDailyEndTime())) {
            inAllowedWindow = true;
            inFinal3Minutes = getMinutesUntilEnd(nowTime, config.getDailyEndTime()) <= 3;
        }

        List<BiddingTimeSlot> slots = slotRepository.findBySlotDateBetween(today.minusDays(1), today);
        for (BiddingTimeSlot slot : slots) {
            LocalDateTime slotStart = LocalDateTime.of(slot.getSlotDate(), slot.getSlotStartTime());
            LocalDateTime slotEnd = slot.getSlotEndTime().isBefore(slot.getSlotStartTime())
                    ? LocalDateTime.of(slot.getSlotDate().plusDays(1), slot.getSlotEndTime())
                    : LocalDateTime.of(slot.getSlotDate(), slot.getSlotEndTime());

            if (!now.isBefore(slotStart) && !now.isAfter(slotEnd)) {
                inAllowedWindow = true;
                inFinal3Minutes = Duration.between(now, slotEnd).toMinutes() <= 3;
                break;
            }
        }

        Optional<Bid> latestBidOpt = bidRepository
            .findTopByProductIdAndBiddingRoundAndLeftBidFalseOrderByBidTimeDesc(productId, product.getCurrentBiddingRound());

        Bid latestBid = latestBidOpt.orElse(null);
        boolean hasOngoingBid = latestBid != null && latestBid.getBidTime().plusMinutes(3).isAfter(now);

        if (!inAllowedWindow && !hasOngoingBid) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You cannot place a new bid right now. Wait for the next bidding window.");
        }

        if (inFinal3Minutes && !hasOngoingBid) {
            return ResponseEntity.badRequest().body("New bids are not allowed in the final 3 minutes.");
        }

        if (latestBid != null && Objects.equals(latestBid.getBuyerId(), buyerId)) {
            return ResponseEntity.badRequest().body("You cannot place consecutive bids in the same round.");
        }

        BigDecimal currentPrice = latestBid != null ? latestBid.getAmount() : product.getPrice();
        BigDecimal minIncrement = product.getMinBidIncrement();

        if (isIncrement) {
            if (bidAmount == null || bidAmount.compareTo(BigDecimal.ZERO) == 0) {
                bidAmount = currentPrice.add(minIncrement);
            } else if (bidAmount.compareTo(currentPrice.add(minIncrement)) < 0) {
                return ResponseEntity.badRequest().body("Bid must be at least ₹" + currentPrice.add(minIncrement));
            }
        } else {
            if (bidAmount == null || bidAmount.compareTo(currentPrice) <= 0) {
                return ResponseEntity.badRequest().body("Bid must be greater than current price: ₹" + currentPrice);
            }
        }

        Bid newBid = new Bid();
        newBid.setBuyerId(buyerId);
        newBid.setProductId(productId);
        newBid.setAmount(bidAmount);
        newBid.setBidIncrement(isIncrement);
        newBid.setBidTime(now);
        newBid.setQuantity(1);
        newBid.setBiddingRound(product.getCurrentBiddingRound());

        product.setCurrentPrice(bidAmount);
        product.setLastBidTime(now);

        bidRepository.save(newBid);
        productRepository.save(product);

        LocalDateTime auctionEndTime = now.plusMinutes(3);

        Map<String, Object> broadcastPayload = new HashMap<>();
        broadcastPayload.put("type", "NEW_BID_PLACED");
        broadcastPayload.put("buyerName", user.getUsername());
        broadcastPayload.put("productId", productId);
        broadcastPayload.put("amount", bidAmount);
        broadcastPayload.put("buyerId", buyerId);
        broadcastPayload.put("timestamp", now.toString());
        broadcastPayload.put("auctionEndTime", auctionEndTime.toString());
        broadcastPayload.put("currentPrice", product.getCurrentPrice());
        broadcastPayload.put("biddingRound", product.getCurrentBiddingRound());
        broadcastPayload.put("event", "BID_UPDATE"); // Optional alias for 'type'
        broadcastPayload.put("bidTime", now.toString()); // Matches 'bidTime' in BidHistory
        broadcastPayload.put("withdrawn", false);

        // ✅ Fix: Notify to product-specific topic
        notificationService.notifyTopic("bid-events/" + productId, broadcastPayload);

        Map<String, Object> privatePayload = new HashMap<>();
        privatePayload.put("type", "BID_CONFIRMATION");
        privatePayload.put("message", "Your bid of ₹" + bidAmount + " has been placed successfully.");
        privatePayload.put("productId", productId);

        notificationService.sendToUser(user.getEmail(), privatePayload);
        
//        getBiddingState(productId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Bid placed successfully");
        response.put("amount", bidAmount);
        response.put("auctionEndTime", auctionEndTime.toString());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }





    // 🔹 2. Finalize Winner
    @Transactional
    @Scheduled(cron = "0,30 * * * * *") // Runs every 30 seconds
    public void finalizeWinner() {
        LocalDateTime now = LocalDateTime.now();

        // Fetch all products currently ACTIVE in auction
        List<Product> activeProducts = productRepository.findByProductStatus(ProductStatus.ACTIVE);

        for (Product product : activeProducts) {
            try {
                // Skip if already finalized for this round
                if (product.isFinalized()) continue;

                int currentRound = product.getCurrentBiddingRound();

                // Fetch latest valid (non-withdrawn, non-winning) bid in the current round only
                Optional<Bid> latestValidBidOpt = bidRepository
                    .findByProductIdAndBiddingRoundOrderByBidTimeDesc(product.getId(), currentRound)
                    .stream()
                    .filter(bid -> !bid.isLeftBid() && !bid.isWinningBid())
                    .findFirst();

                // If no valid bids found for current round, skip finalizing this product now
                if (latestValidBidOpt.isEmpty()) {
                    continue;
                }

                Bid latestBid = latestValidBidOpt.get();

                // Check if the 3 minutes bidding window after the bid time has elapsed
                LocalDateTime bidEndTime = latestBid.getBidTime().plusMinutes(3);
                if (now.isBefore(bidEndTime)) {
                    System.out.println("⏳ Not finalizing yet for Product " + product.getId() +
                            " | Current Time: " + now +
                            " | Bid End Time: " + bidEndTime);
                    continue;
                }

                // Fetch the winning user
                Optional<User> userOpt = userRepository.findById(latestBid.getBuyerId());
                if (userOpt.isEmpty()) {
                    System.err.println("❌ User not found for winning bid. Product ID: " + product.getId());
                    continue;
                }

                User user = userOpt.get();

                // Mark bid as winning bid
                latestBid.setWinningBid(true);
                bidRepository.save(latestBid);

                int currentStock = product.getStock();
                if (currentStock <= 0) {
                    // No stock left, skip
                    continue;
                }

                int newStock = currentStock - 1;
                product.setStock(newStock);
                product.setLastWinningBidId(latestBid.getId());
                product.setUpdatedAt(now);

                // Mark product finalized for this round
                product.setFinalized(true);

                if (newStock > 0) {
                    // More stock left — prepare for next round
                    product.setCurrentBiddingRound(currentRound + 1);
                    product.setFinalized(false); // reactivate for next round
                    product.setProductStatus(ProductStatus.ACTIVE);
                    product.setLastWinningBidId(null); // reset last winner for next round
                } else {
                    // No stock left — mark product sold
                    product.setProductStatus(ProductStatus.SOLD);
                }

                productRepository.saveAndFlush(product);

                // Fetch all product images (for email)
                List<ProductImage> images = productImageRepository.findByProductId(product.getId());

                // Send winner email
                emailService.sendWinnerEmail(user, product, images, latestBid);

                // Notify all subscribers about product sold (if sold)
                Map<String, Object> soldPayload = new HashMap<>();
                soldPayload.put("type", "PRODUCT_SOLD");
                soldPayload.put("productId", product.getId());
                soldPayload.put("productName", product.getName());
                soldPayload.put("timestamp", now.toString());
                notificationService.notifyTopic("bid-events/" + product.getId(), soldPayload);
                System.out.println("📢 PRODUCT_SOLD broadcasted for Product ID: " + product.getId());

                // Notify all subscribers about bid winner announcement
                Map<String, Object> broadcastPayload = new HashMap<>();
                broadcastPayload.put("type", "BID_WINNER_ANNOUNCED");
                broadcastPayload.put("productId", product.getId());
                broadcastPayload.put("round", product.getCurrentBiddingRound());
                broadcastPayload.put("winnerUsername", user.getUsername());
                broadcastPayload.put("winningAmount", latestBid.getAmount());
                broadcastPayload.put("timestamp", now.toString());
                broadcastPayload.put("buyerId", user.getId());
                broadcastPayload.put("stock", newStock);
                notificationService.notifyTopic("bid-events/" + product.getId(), broadcastPayload);
                System.out.println("🏆 BID_WINNER_ANNOUNCED for Product ID: " + product.getId());

                // Send private WebSocket notification to winner
                Map<String, Object> privatePayload = new HashMap<>();
                privatePayload.put("type", "WINNING_NOTIFICATION");
                privatePayload.put("message", "Congratulations! You won the bid for " + product.getName() +
                        " (Product ID: " + product.getId() + ") with amount ₹" + latestBid.getAmount());
                privatePayload.put("productId", product.getId());
                privatePayload.put("amount", latestBid.getAmount());
                privatePayload.put("stock", newStock);
                notificationService.sendToUser(user.getEmail(), privatePayload);
                System.out.println("📩 WINNING_NOTIFICATION sent to user: " + user.getUsername());

                System.out.println("✔ Finalized Product " + product.getId() +
                        " | Winner: " + user.getUsername() +
                        " | Round: " + currentRound +
                        " | Remaining Stock: " + newStock);

            } catch (Exception e) {
                System.err.println("❌ Finalization failed for Product ID: " + product.getId() +
                        " | Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }






    public ResponseEntity<?> getLiveBiddingProducts() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime nowTime = now.toLocalTime();
        LocalDate today = now.toLocalDate();

        List<Product> activeProducts = productRepository.findByProductStatus(ProductStatus.ACTIVE);
        if (activeProducts == null || activeProducts.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Map<String, Object>> liveProductDetails = new ArrayList<>();

        // ✅ Validate daily config
        DailyBiddingTimeConfig config = dailyConfigRepository.findById(1).orElse(null);
        boolean inDailyWindow = false;
        if (config != null && config.getDailyStartTime() != null && config.getDailyEndTime() != null) {
            inDailyWindow = isWithinTimeWindow(nowTime, config.getDailyStartTime(), config.getDailyEndTime());
        }

        // ✅ Validate time slots for today
        List<BiddingTimeSlot> slotsToday = slotRepository.findBySlotDate(today);
        List<BiddingTimeSlot> activeSlots = new ArrayList<>();
        if (slotsToday != null && !slotsToday.isEmpty()) {
            activeSlots = slotsToday.stream()
                    .filter(slot -> slot.getSlotStartTime() != null && slot.getSlotEndTime() != null &&
                            isWithinTimeWindow(nowTime, slot.getSlotStartTime(), slot.getSlotEndTime()))
                    .toList();
        }

        for (Product product : activeProducts) {
            if (product == null || product.isFinalized()) continue;

            boolean isLive = inDailyWindow || !activeSlots.isEmpty();

            // ✅ Check recent bid activity (grace period)
            if (!isLive) {
                Bid latestBid = bidRepository.findTopByProductIdAndBiddingRoundOrderByBidTimeDesc(
                        product.getId(), product.getCurrentBiddingRound());
                if (latestBid != null && latestBid.getBidTime() != null &&
                    latestBid.getBidTime().plusMinutes(3).isAfter(now)) {
                    isLive = true;
                }
            }

            if (isLive) {
                // ✅ Fetch product images safely
                List<ProductImage> images = productImageRepository.findByProductId(product.getId());
                List<String> imageUrls = (images != null) ?
                        images.stream()
                                .map(ProductImage::getImageUrl)
                                .filter(Objects::nonNull)
                                .toList()
                        : new ArrayList<>();

                // ✅ Build product response map
                Map<String, Object> productData = new HashMap<>();
                productData.put("id", product.getId());
                productData.put("name", product.getName());
                productData.put("price", product.getPrice());
                productData.put("stock", product.getStock());
                productData.put("minBidIncrement", product.getMinBidIncrement());
                productData.put("currentRound", product.getCurrentBiddingRound());
                productData.put("status", product.getProductStatus() != null ? product.getProductStatus().toString() : "UNKNOWN");
                productData.put("isFinalized", product.isFinalized());
                productData.put("imageUrls", imageUrls);

                liveProductDetails.add(productData);
            }
        }

        // ✅ WebSocket broadcast
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "LIVE_BIDDING_PRODUCTS");
        payload.put("timestamp", now.toString());
        payload.put("products", liveProductDetails);

        notificationService.notifyTopic("live-products", payload);

        return ResponseEntity.ok(liveProductDetails);
    }

    // 🔹 4. Reschedule Unbidded Products
//    @Scheduled(cron = "0 0/1 * * * *")
//    @Transactional
//    public void rescheduleUnbiddedProducts() {
//        LocalDateTime now = LocalDateTime.now();
//        LocalTime nowTime = now.toLocalTime();
//        LocalDate today = now.toLocalDate();
//
//        Optional<DailyBiddingTimeConfig> configOpt = dailyConfigRepository.findById(1);
//        if (configOpt.isEmpty()) return;
//        DailyBiddingTimeConfig config = configOpt.get();
//
//        List<BiddingTimeSlot> slotsToday = slotRepository.findBySlotDate(today);
//        List<Product> allProducts = productRepository.findByStockGreaterThan(0);
//
//        List<Product> activatedProducts = new ArrayList<>();
//        List<Map<String, Object>> rescheduledPayloadList = new ArrayList<>();
//
//        for (Product product : allProducts) {
//            boolean withinDaily = isWithinTimeWindow(nowTime, config.getDailyStartTime(), config.getDailyEndTime());
//            boolean withinSlot = slotsToday.stream()
//                    .anyMatch(slot -> isWithinTimeWindow(nowTime, slot.getSlotStartTime(), slot.getSlotEndTime()));
//            boolean currentlyActiveTime = withinDaily || withinSlot;
//
//            Bid latestBid = bidRepository.findTopByProductIdAndBiddingRoundOrderByBidTimeDesc(
//                    product.getId(), product.getCurrentBiddingRound());
//            boolean hasRecentBid = latestBid != null && latestBid.getBidTime().plusMinutes(3).isAfter(now);
//
//            if (hasRecentBid || currentlyActiveTime) {
//                if (setActiveIfNeeded(product, now)) {
//                    activatedProducts.add(product); // Collect for broadcast
//                }
//                continue;
//            }
//
//            // Outside active time — check if already rescheduled
//            LocalDateTime lastRescheduled = product.getLastRescheduledAt();
//            LocalDateTime currentWindowEnd = getCurrentWindowEnd(config, slotsToday, now);
//
//            if (lastRescheduled != null && lastRescheduled.isAfter(currentWindowEnd)) {
//                continue; // Already rescheduled for this window
//            }
//
//            if (setInactiveIfNeeded(product, now)) {
//                LocalDateTime scheduledAt;
//                Optional<BiddingTimeSlot> nextSlotOpt = slotsToday.stream()
//                        .filter(slot -> slot.getSlotStartTime().isAfter(nowTime))
//                        .min(Comparator.comparing(BiddingTimeSlot::getSlotStartTime));
//
//                if (nextSlotOpt.isPresent()) {
//                    scheduledAt = LocalDateTime.of(today, nextSlotOpt.get().getSlotStartTime());
//                } else if (config.getDailyStartTime().isAfter(nowTime)) {
//                    scheduledAt = LocalDateTime.of(today, config.getDailyStartTime());
//                } else {
//                    scheduledAt = LocalDateTime.of(today.plusDays(1), config.getDailyStartTime());
//                }
//
//                product.setLastRescheduledAt(now);
//                productRepository.save(product);
//
//                Map<String, Object> productInfo = new HashMap<>();
//                productInfo.put("id", product.getId());
//                productInfo.put("name", product.getName());
//                productInfo.put("scheduledAt", scheduledAt.toString());
//
//                rescheduledPayloadList.add(productInfo);
//
//                System.out.println("⏳ Product " + product.getId() + " rescheduled for: " + scheduledAt);
//            }
//        }
//
//        // 🔔 WebSocket broadcast: product(s) ACTIVATED
//        if (!activatedProducts.isEmpty()) {
//            List<Map<String, Object>> activatedPayload = activatedProducts.stream().map(p -> {
//                Map<String, Object> map = new HashMap<>();
//                map.put("id", p.getId());
//                map.put("name", p.getName());
//                map.put("status", p.getProductStatus().toString());
//                return map;
//            }).toList();
//
//            Map<String, Object> activationPayload = new HashMap<>();
//            activationPayload.put("type", "PRODUCTS_ACTIVATED");
//            activationPayload.put("timestamp", now.toString());
//            activationPayload.put("products", activatedPayload);
//
//            notificationService.broadcastToTopic("/topic/product-activity", activationPayload);
//        }
//
//        // 🔔 WebSocket broadcast: product(s) RESCHEDULED
//        if (!rescheduledPayloadList.isEmpty()) {
//            Map<String, Object> reschedulePayload = new HashMap<>();
//            reschedulePayload.put("type", "PRODUCTS_RESCHEDULED");
//            reschedulePayload.put("timestamp", now.toString());
//            reschedulePayload.put("products", rescheduledPayloadList);
//
//            notificationService.broadcastToTopic("/topic/product-activity", reschedulePayload);
//        }
//    }
    
    @Transactional
    public void rescheduleUnbiddedProducts() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime nowTime = now.toLocalTime();
        LocalDate today = now.toLocalDate();

        Optional<DailyBiddingTimeConfig> configOpt = dailyConfigRepository.findById(1);
        if (configOpt.isEmpty()) return;
        DailyBiddingTimeConfig config = configOpt.get();

        List<BiddingTimeSlot> slotsToday = slotRepository.findBySlotDate(today);
        List<Product> allProducts = productRepository.findByStockGreaterThan(0);

        List<Product> activatedProducts = new ArrayList<>();
        List<Map<String, Object>> rescheduledPayloadList = new ArrayList<>();

        for (Product product : allProducts) {
            boolean withinDaily = isWithinTimeWindow(nowTime, config.getDailyStartTime(), config.getDailyEndTime());
            boolean withinSlot = slotsToday.stream()
                    .anyMatch(slot -> isWithinTimeWindow(nowTime, slot.getSlotStartTime(), slot.getSlotEndTime()));
            boolean currentlyActiveTime = withinDaily || withinSlot;

            Bid latestBid = bidRepository.findTopByProductIdAndBiddingRoundOrderByBidTimeDesc(
                    product.getId(), product.getCurrentBiddingRound());
            boolean hasRecentBid = latestBid != null && latestBid.getBidTime().plusMinutes(3).isAfter(now);

            if (hasRecentBid || currentlyActiveTime) {
                if (setActiveIfNeeded(product, now)) {
                    activatedProducts.add(product);
                }
                continue;
            }

            LocalDateTime lastRescheduled = product.getLastRescheduledAt();
            LocalDateTime currentWindowEnd = getCurrentWindowEnd(config, slotsToday, now);

            if (lastRescheduled != null && lastRescheduled.isAfter(currentWindowEnd)) {
                continue;
            }

            if (setInactiveIfNeeded(product, now)) {
                LocalDateTime scheduledAt;
                Optional<BiddingTimeSlot> nextSlotOpt = slotsToday.stream()
                        .filter(slot -> slot.getSlotStartTime().isAfter(nowTime))
                        .min(Comparator.comparing(BiddingTimeSlot::getSlotStartTime));

                if (nextSlotOpt.isPresent()) {
                    scheduledAt = LocalDateTime.of(today, nextSlotOpt.get().getSlotStartTime());
                } else if (config.getDailyStartTime().isAfter(nowTime)) {
                    scheduledAt = LocalDateTime.of(today, config.getDailyStartTime());
                } else {
                    scheduledAt = LocalDateTime.of(today.plusDays(1), config.getDailyStartTime());
                }

                product.setLastRescheduledAt(now);
                productRepository.save(product);

                Map<String, Object> productInfo = new HashMap<>();
                productInfo.put("id", product.getId());
                productInfo.put("name", product.getName());
                productInfo.put("scheduledAt", scheduledAt.toString());

                rescheduledPayloadList.add(productInfo);

                System.out.println("⏳ Product " + product.getId() + " rescheduled for: " + scheduledAt);
            }
        }

        if (!activatedProducts.isEmpty()) {
            List<Map<String, Object>> activatedPayload = activatedProducts.stream().map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("name", p.getName());
                map.put("status", p.getProductStatus().toString());
                return map;
            }).toList();

            Map<String, Object> activationPayload = new HashMap<>();
            activationPayload.put("type", "PRODUCTS_ACTIVATED");
            activationPayload.put("timestamp", now.toString());
            activationPayload.put("products", activatedPayload);

            notificationService.broadcastToTopic("/topic/product-activity", activationPayload);
        }

        if (!rescheduledPayloadList.isEmpty()) {
            Map<String, Object> reschedulePayload = new HashMap<>();
            reschedulePayload.put("type", "PRODUCTS_RESCHEDULED");
            reschedulePayload.put("timestamp", now.toString());
            reschedulePayload.put("products", rescheduledPayloadList);

            notificationService.broadcastToTopic("/topic/product-activity", reschedulePayload);
        }
    }

    private LocalDateTime getCurrentWindowEnd(DailyBiddingTimeConfig config, List<BiddingTimeSlot> slotsToday, LocalDateTime now) {
        LocalTime nowTime = now.toLocalTime();
        LocalDateTime windowEnd;

        Optional<BiddingTimeSlot> currentSlot = slotsToday.stream()
                .filter(slot -> isWithinTimeWindow(nowTime, slot.getSlotStartTime(), slot.getSlotEndTime()))
                .findFirst();

        if (currentSlot.isPresent()) {
            windowEnd = LocalDateTime.of(now.toLocalDate(), currentSlot.get().getSlotEndTime());
        } else if (isWithinTimeWindow(nowTime, config.getDailyStartTime(), config.getDailyEndTime())) {
            windowEnd = LocalDateTime.of(now.toLocalDate(), config.getDailyEndTime());
        } else {
            windowEnd = now.minusYears(10); // fallback
        }

        // 🔔 WebSocket broadcast for window end
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "CURRENT_WINDOW_END");
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("windowEnd", windowEnd.toString());

        notificationService.broadcastToTopic("/topic/bidding/window", payload);

        return windowEnd;
    }


    private boolean setActiveIfNeeded(Product product, LocalDateTime now) {
        if (product.getProductStatus() != ProductStatus.ACTIVE) {
            product.setProductStatus(ProductStatus.ACTIVE);
            product.setUpdatedAt(now);
            productRepository.save(product);
            return true;
        }
        return false;
    }

    private boolean setInactiveIfNeeded(Product product, LocalDateTime now) {
        if (product.getProductStatus() != ProductStatus.INACTIVE) {
            product.setProductStatus(ProductStatus.INACTIVE);
            product.setUpdatedAt(now);
            productRepository.save(product);
            return true;
        }
        return false;
    }

    // Helper method inside the service (or move it elsewhere)
    boolean isWithinTimeWindow(LocalTime current, LocalTime start, LocalTime end) {
        if (start.equals(end)) {
            return true; // 24-hour window if start == end
        }
        if (start.isBefore(end)) {
            // Normal time window, e.g. 08:00 to 17:00
            return !current.isBefore(start) && !current.isAfter(end);
        } else {
            // Overnight time window, e.g. 23:15 to 01:00
            return !current.isBefore(start) || !current.isAfter(end);
        }
    }
    private long getMinutesUntilEnd(LocalTime now, LocalTime end) {
        if (end.isAfter(now)) {
            return Duration.between(now, end).toMinutes();
        } else {
            // Overnight case: e.g., 11 PM to 1 AM
            return Duration.between(now, LocalTime.MIDNIGHT).toMinutes()
                    + Duration.between(LocalTime.MIN, end).toMinutes();
        }
    }

    
    // Get all bids for a product
    public ResponseEntity<?> getAllBidsForProduct(int productId) {
        Optional<Product> productOpt = productRepository.findById(productId);

        if (productOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Product with ID " + productId + " not found.");
        }

        Product product = productOpt.get();
        List<Bid> bids = bidRepository.findByProductIdOrderByBidTimeDesc(productId); // Order latest first

        if (bids.isEmpty()) {
            return ResponseEntity.ok("No bids have been placed for this product yet.");
        }

        // Fetch buyer names in batch
        Set<Integer> buyerIds = bids.stream()
                .map(Bid::getBuyerId)
                .collect(Collectors.toSet());

        Map<Integer, String> buyerIdToName = userRepository.findAllById(buyerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        List<BidResponseDTO> response = bids.stream().map(bid -> {
            String buyerName = buyerIdToName.getOrDefault(bid.getBuyerId(), "Unknown");

            return new BidResponseDTO(
                    bid.getId(),
                    bid.getBuyerId(),
                    buyerName,
                    product.getId(),
                    product.getName(),
                    bid.getAmount(),
                    bid.isBidIncrement(),
                    bid.getBidTime(),
                    bid.isLeftBid(),
                    bid.getBiddingRound()// ✅ Include withdrawn status
            );
        }).collect(Collectors.toList());

        // 🔔 WebSocket broadcast to product-specific topic
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "BID_HISTORY");
        payload.put("productId", productId);
        payload.put("bids", response);
        payload.put("timestamp", LocalDateTime.now().toString());

        notificationService.broadcastToTopic("/topic/product/" + productId + "/bids", payload);

        return ResponseEntity.ok(response);
    }

    
//    public ResponseEntity<?> getBiddingState(int productId) {
//        Optional<Product> productOpt = productRepository.findById(productId);
//        if (productOpt.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body("Product with ID " + productId + " not found.");
//        }
//
//        Product product = productOpt.get();
//
//        Optional<Bid> latestBidOpt = bidRepository
//                .findTopByProductIdAndLeftBidFalseOrderByBiddingRoundDescBidTimeDesc(productId);
//        if (latestBidOpt.isEmpty()) {
//            return ResponseEntity.badRequest().body("No active bidding round for this product.");
//        }
//
//        Bid latestBid = latestBidOpt.get();
//        int currentRound = latestBid.getBiddingRound();
//
//        LocalDateTime auctionEndTime = latestBid.getBidTime().plusMinutes(3);
//
//        List<Bid> bids = bidRepository.findByProductIdAndBiddingRoundAndLeftBidFalseOrderByBidTimeDesc(productId, currentRound);
//
//        Set<Integer> buyerIds = bids.stream().map(Bid::getBuyerId).collect(Collectors.toSet());
//        Map<Integer, String> buyerIdToName = userRepository.findAllById(buyerIds).stream()
//                .collect(Collectors.toMap(User::getId, User::getUsername));
//
//        List<BidResponseDTO> bidHistory = bids.stream()
//                .map(bid -> {
//                    String buyerName = buyerIdToName.getOrDefault(bid.getBuyerId(), "Unknown");
//                    return new BidResponseDTO(
//                            bid.getId(),
//                            bid.getBuyerId(),
//                            buyerName,
//                            product.getId(),
//                            product.getName(),
//                            bid.getAmount(),
//                            bid.isBidIncrement(),
//                            bid.getBidTime(),
//                            false
//                    );
//                })
//                .collect(Collectors.toList());
//
//        Optional<BidResponseDTO> highestBid = bidHistory.stream()
//                .max(Comparator.comparing(BidResponseDTO::getAmount));
//
//        // Final response
//        Map<String, Object> response = new HashMap<>();
//        response.put("auctionEndTime", auctionEndTime);
//        response.put("currentBiddingRound", currentRound);
//        response.put("currentPrice", product.getCurrentPrice());
//        response.put("bidHistory", bidHistory);
//        response.put("highestBid", highestBid.orElse(null));
//
//        // ✅ WebSocket payload (fix topic)
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("type", "BID_STATE");
//        payload.put("productId", productId);
//        payload.put("bids", bidHistory);
//        payload.put("highestBid", highestBid.orElse(null));
//        payload.put("currentPrice", product.getCurrentPrice());
//        payload.put("currentBiddingRound", currentRound);
//        payload.put("auctionEndTime", auctionEndTime.toString());
//        payload.put("timestamp", LocalDateTime.now().toString());
//
//        notificationService.notifyTopic("bid-events/" + productId, payload); // ✅ FIXED TOPIC
//
//        return ResponseEntity.ok(response);
//    }
//




    // Remove a buyer's bid if they want to leave
    @Transactional
    public ResponseEntity<?> withdrawFromAuction(int buyerId, int productId) {

        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found.");
        }

        List<Bid> bids = bidRepository.findByProductIdOrderByBidTimeDesc(productId);
        if (bids.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No bids found for this product.");
        }

        // Find buyer's latest valid bid (not withdrawn)
        Optional<Bid> lastBidByBuyerOpt = bids.stream()
                .filter(bid -> bid.getBuyerId() == buyerId && !bid.isLeftBid())
                .findFirst();

        if (lastBidByBuyerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Buyer has not placed any valid bid for this product.");
        }

        // Mark the latest bid as withdrawn
        Bid lastBidByBuyer = lastBidByBuyerOpt.get();
        lastBidByBuyer.setLeftBid(true);
        bidRepository.save(lastBidByBuyer);

        // Re-fetch bids
        bids = bidRepository.findByProductIdOrderByBidTimeDesc(productId);

        // Find latest valid (non-withdrawn) bid
        Optional<Bid> latestValidBidOpt = bids.stream()
                .filter(bid -> !bid.isLeftBid())
                .max(Comparator.comparing(Bid::getBidTime));

        Product product = productOpt.get();

        if (latestValidBidOpt.isPresent() && latestValidBidOpt.get().getBuyerId() == buyerId) {
            // Top bidder withdrew
            Optional<Bid> newTopBidOpt = bids.stream()
                    .filter(bid -> bid.getBuyerId() != buyerId && !bid.isLeftBid())
                    .max(Comparator.comparing(Bid::getBidTime));

            if (newTopBidOpt.isPresent()) {
                Bid newTopBid = newTopBidOpt.get();
                product.setLastWinningBidId(newTopBid.getId());
                product.setUpdatedAt(LocalDateTime.now());
                productRepository.save(product);
                auctionTimerService.restartTimer(productId);

                String newTopUsername = (newTopBid.getBuyer() != null)
                        ? newTopBid.getBuyer().getUsername()
                        : "Unknown Buyer";

                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "TOP_BIDDER_WITHDREW");
                payload.put("productId", productId);
                payload.put("newTopBidderId", newTopBid.getBuyerId());
                payload.put("username", newTopUsername);
                payload.put("newTopBidAmount", newTopBid.getAmount());
                payload.put("timestamp", LocalDateTime.now().toString());

                notificationService.notifyTopic("bid-events/" + productId, payload);

                return ResponseEntity.ok("Withdrawn. You were the top bidder; bid reassigned to previous bidder.");
            } else {
                // No other valid bids left
                product.setLastWinningBidId(null);
                product.setUpdatedAt(LocalDateTime.now());
                productRepository.save(product);
                auctionTimerService.cancelTimer(productId);

                Bid topBid = latestValidBidOpt.get();
                String topUsername = (topBid.getBuyer() != null)
                        ? topBid.getBuyer().getUsername()
                        : "Unknown Buyer";

                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "TOP_BIDDER_WITHDREW_NO_BIDDERS_LEFT");
                payload.put("productId", productId);
                payload.put("username", topUsername);
                payload.put("timestamp", LocalDateTime.now().toString());

                notificationService.notifyTopic("bid-events/" + productId, payload);

                return ResponseEntity.ok("Withdrawn. No other bidders remaining.");
            }
        }

        // Non-top bidder withdrew
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "NON_TOP_BIDDER_WITHDREW");
        payload.put("productId", productId);
        payload.put("buyerId", buyerId);
        payload.put("timestamp", LocalDateTime.now().toString());

        notificationService.notifyTopic("bid-events/" + productId, payload);

        return ResponseEntity.ok("Withdrawn successfully. You were not the top bidder.");
    }


      
    
    // Finalize the winner after the bidding period ends
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFinalizedBidDetails(int productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found!");
        }

        Product product = productOpt.get();

        if (!product.isFinalized() || product.getLastWinningBidId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Product not finalized yet.");
        }

        Optional<Bid> bidOpt = bidRepository.findById(product.getLastWinningBidId());
        if (bidOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Winning bid not found.");
        }

        Bid winningBid = bidOpt.get();

        Optional<User> winnerOpt = userRepository.findById(winningBid.getBuyerId());
        if (winnerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Winning user not found.");
        }

        User winner = winnerOpt.get();

        Map<String, Object> response = new HashMap<>();
        response.put("winner", winner.getUsername());
        response.put("quantity", winningBid.getQuantity());
        response.put("bidAmount", winningBid.getAmount());
        response.put("bidTime", winningBid.getBidTime());
        response.put("productId", productId);
        response.put("remainingStock", product.getStock());

        // 🔔 WebSocket Notification to all subscribers (e.g., admin/seller view)
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "PRODUCT_FINALIZED");
        payload.put("productId", productId);
        payload.put("winner", winner.getUsername());
        payload.put("bidAmount", winningBid.getAmount());
        payload.put("bidTime", winningBid.getBidTime().toString());
        payload.put("quantity", winningBid.getQuantity());
        payload.put("timestamp", LocalDateTime.now().toString());

        notificationService.broadcastToTopic("/topic/finalized-products", payload);

        return ResponseEntity.ok(response);
    }

 
    
    public boolean isBiddingLiveNow() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        boolean isLive = false;

        // ✅ Check global daily bidding time config
        Optional<DailyBiddingTimeConfig> configOpt = dailyConfigRepository.findById(1);
        if (configOpt.isPresent()) {
            DailyBiddingTimeConfig config = configOpt.get();
            LocalTime start = config.getDailyStartTime();
            LocalTime end = config.getDailyEndTime();

            if (start != null && end != null && !now.isBefore(start) && !now.isAfter(end)) {
                isLive = true;
            }
        }

        // ✅ Check today's slots
        if (!isLive) {
            List<BiddingTimeSlot> todaySlots = slotRepository.findBySlotDate(today);
            for (BiddingTimeSlot slot : todaySlots) {
                if (slot.getSlotStartTime() != null && slot.getSlotEndTime() != null) {
                    if (!now.isBefore(slot.getSlotStartTime()) && !now.isAfter(slot.getSlotEndTime())) {
                        isLive = true;
                        break;
                    }
                }
            }
        }

        // 🔔 Optional: Broadcast if live (you can move this to a scheduler if needed)
        if (isLive) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "BIDDING_STATUS");
            payload.put("status", "LIVE");
            payload.put("timestamp", LocalDateTime.now().toString());

            notificationService.broadcastToTopic("/topic/bidding-status", payload);
        }

        return isLive;
    }

    
    public List<Winner> getAllWinners() {
        List<Winner> winners = new ArrayList<>();
        List<Product> products = productRepository.findAll();

        for (Product product : products) {
            int productId = product.getId();
            String productName = product.getName();

            List<Integer> rounds = bidRepository.findDistinctRoundsByProductId(productId);

            for (int round : rounds) {
                List<Bid> bids = bidRepository.findAllByProductIdAndRound(productId, round);

                Optional<Bid> topBidOpt = bids.stream()
                    .sorted(Comparator.comparing(Bid::getAmount).reversed()
                                      .thenComparing(Bid::getBidTime))
                    .findFirst();

                if (topBidOpt.isPresent()) {
                    Bid topBid = topBidOpt.get();
                    Optional<User> buyerOpt = userRepository.findById(topBid.getBuyerId());

                    String buyerName = "Unknown";
                    Long buyerContact = null;

                    if (buyerOpt.isPresent()) {
                        User buyer = buyerOpt.get();
                        if (buyer.getUsername() != null) {
                            buyerName = buyer.getUsername();
                        }
                        buyerContact = buyer.getContact(); // Long
                    }

                    winners.add(new Winner(
                        productId,
                        productName,
                        round,
                        topBid.getBuyerId(),
                        buyerName,
                        buyerContact,
                        topBid.getAmount(),
                        topBid.getBidTime()
                    ));
                }
            }
        }
        
        if (!winners.isEmpty()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "ALL_WINNERS_LIST");
            payload.put("timestamp", LocalDateTime.now().toString());
            payload.put("winners", winners);

            notificationService.broadcastToTopic("/topic/all-winners", payload);
        }

        return winners;
    }


    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllWinnersForProduct(int productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found!");
        }

        Product product = productOpt.get();
        String productName = product.getName();

        List<Integer> rounds = bidRepository.findDistinctRoundsByProductId(productId);
        List<Winner> winners = new ArrayList<>();

        for (Integer round : rounds) {
            List<Bid> bids = bidRepository.findAllByProductIdAndBiddingRoundOrderByAmountDescTimeAsc(productId, round);
            if (!bids.isEmpty()) {
                Bid topBid = bids.get(0); // highest amount, earliest time in case of tie
                Optional<User> userOpt = userRepository.findById(topBid.getBuyerId());

                String buyerName = "Unknown";
                Long buyerContact = null;

                if (userOpt.isPresent()) {
                    User u = userOpt.get();
                    buyerName = u.getUsername() != null ? u.getUsername() : "Unknown";
                    buyerContact = u.getContact();
                }

                Winner winner = new Winner(
                    productId,
                    productName,
                    round,
                    topBid.getBuyerId(),
                    buyerName,
                    buyerContact,
                    topBid.getAmount(),
                    topBid.getBidTime()
                );

                winners.add(winner);
            }
        }

        if (winners.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No winners found for this product.");
        }
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ALL_PRODUCT_WINNERS");
        payload.put("productId", productId);
        payload.put("productName", productName);
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("winners", winners);

        notificationService.broadcastToTopic("/topic/product-winners", payload);


        return ResponseEntity.ok(winners);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getWinningBidsByBuyerId(int buyerId) {
        Optional<User> userOpt = userRepository.findById(buyerId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        List<Bid> winningBids = bidRepository.findByBuyerIdAndWinningBidTrue(buyerId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Bid bid : winningBids) {
            Optional<Product> productOpt = productRepository.findById(bid.getProductId());
            if (productOpt.isEmpty()) continue;

            Product product = productOpt.get();

            List<ProductImage> images = productImageRepository.findByProductId(product.getId());
            List<String> imageUrls = images.stream()
                    .map(ProductImage::getImageUrl)
                    .collect(Collectors.toList());

            Map<String, Object> bidDetails = new HashMap<>();
            bidDetails.put("productId", product.getId());
            bidDetails.put("productName", product.getName());
            bidDetails.put("description", product.getDescription());
            bidDetails.put("category", product.getCategory());
            bidDetails.put("stock", product.getStock());
            bidDetails.put("images", imageUrls);

            bidDetails.put("bidId", bid.getId());
            bidDetails.put("bidAmount", bid.getAmount());
            bidDetails.put("quantity", bid.getQuantity());
            bidDetails.put("bidTime", bid.getBidTime());
            bidDetails.put("biddingRound", bid.getBiddingRound());

            result.add(bidDetails);
        }

        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No winning bids found for this buyer.");
        }

        return ResponseEntity.ok(result);
    }




}