package com.example.flexbid.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flexbid.dto.ProductRequest;
import com.example.flexbid.dto.ProductResponse;
import com.example.flexbid.dto.ProductUpdateRequest;
import com.example.flexbid.model.BiddingTimeSlot;
import com.example.flexbid.model.DailyBiddingTimeConfig;
import com.example.flexbid.model.Product;
import com.example.flexbid.model.ProductImage;
import com.example.flexbid.model.ProductStatus;
import com.example.flexbid.model.Role;
import com.example.flexbid.model.User;
import com.example.flexbid.repository.BidRepository;
import com.example.flexbid.repository.BiddingTimeSlotRepository;
import com.example.flexbid.repository.DailyBiddingTimeConfigRepository;
import com.example.flexbid.repository.ProductImageRepository;
import com.example.flexbid.repository.ProductRepository;
import com.example.flexbid.repository.UserRepository;
import com.example.flexbid.repository.UserRoleRepository;

@Service
public class ProductService {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductImageRepository productImageRepository;

	@Autowired
	private DailyBiddingTimeConfigRepository dailyConfigRepository;

	@Autowired
	private BiddingTimeSlotRepository slotRepository;
	
	 @Autowired
	 private UserRepository userRepo;
	 
	 @Autowired
	 private BidRepository bidRepository;
	 
	 @Autowired
	 private UserRoleRepository userRoleRepo;
	 
	 @Autowired
	 private BidService bidService;
	 
	 @Autowired
	 private WebSocketNotificationService notificationService;

	 @Transactional
	 public ResponseEntity<?> addProduct(ProductRequest request) {
	     Optional<User> sellerOpt = userRepo.findById(request.getSellerId());
	     if (!sellerOpt.isPresent()) {
	         return ResponseEntity.badRequest().body("Seller not found.");
	     }

	     User seller = sellerOpt.get();
	     
	     boolean isSeller = userRoleRepo.existsByUserIdAndRole(sellerOpt.get().getId(), Role.SELLER);
	     if (!isSeller) {
	         return ResponseEntity.badRequest().body("User is not a seller.");
	     }

	     if (request.getName() == null || request.getName().trim().isEmpty()) {
	         return ResponseEntity.badRequest().body("Product name is required.");
	     }
	     if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
	         return ResponseEntity.badRequest().body("Price must be positive.");
	     }
	     if (request.getMinBidIncrement() == null || request.getMinBidIncrement().compareTo(BigDecimal.ZERO) <= 0) {
	         return ResponseEntity.badRequest().body("Minimum bid increment must be positive.");
	     }
	     if (request.getStock() < 1) {
	         return ResponseEntity.badRequest().body("Stock must be at least 1.");
	     }

	     LocalDateTime now = LocalDateTime.now();

	     Product product = new Product();
	     product.setSeller(seller);
	     product.setName(request.getName().trim());
	     product.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
	     product.setCategory(request.getCategory() != null ? request.getCategory().trim() : null);
	     product.setBrand(request.getBrand() != null ? request.getBrand().trim() : null);
	     product.setPrice(request.getPrice());
	     product.setMinBidIncrement(request.getMinBidIncrement());
	     product.setStock(request.getStock());
	     product.setProductStatus(ProductStatus.ACTIVE);
	     product.setCreatedAt(now);
	     product.setUpdatedAt(now);
	     product.setCurrentBiddingRound(1); // start at round 1

	     Product savedProduct = productRepository.save(product);

	     List<String> imageUrls = new ArrayList<>();
	     if (request.getImageUrls() != null) {
	         for (String imageUrl : request.getImageUrls()) {
	             if (imageUrl != null && !imageUrl.trim().isEmpty()) {
	                 ProductImage img = new ProductImage();
	                 img.setProductId(savedProduct.getId());
	                 img.setImageUrl(imageUrl.trim());
	                 img.setUploadedAt(now);
	                 productImageRepository.save(img);
	                 imageUrls.add(imageUrl.trim());
	             }
	         }
	     }
	     
	     
	     Map<String, Object> payload = new HashMap<>();
	     payload.put("type", "NEW_PRODUCT");
	     payload.put("productId", savedProduct.getId());
	     payload.put("name", savedProduct.getName());
	     payload.put("price", savedProduct.getPrice());
	     payload.put("stock", savedProduct.getStock());
	     payload.put("createdAt", savedProduct.getCreatedAt().toString());
	     payload.put("imageUrls", imageUrls);

	     notificationService.notifyTopic("products", payload);

	     return ResponseEntity.status(HttpStatus.CREATED)
	             .body("Product added successfully with ID: " + savedProduct.getId());
	 }

	 public ResponseEntity<?> getAllProductsWithImages() {
		    List<Product> products = productRepository.findAll();

		    List<ProductResponse> responseList = new ArrayList<>();

		    for (Product product : products) {
		        ProductResponse response = new ProductResponse();
		        response.setId(product.getId());
		        response.setSeller(product.getSeller().getId());
		        response.setName(product.getName());
		        response.setDescription(product.getDescription());
		        response.setCategory(product.getCategory());
		        response.setBrand(product.getBrand());
		        response.setPrice(product.getPrice());
		        response.setMinBidIncrement(product.getMinBidIncrement());
		        response.setProductStatus(product.getProductStatus());
		        response.setStock(product.getStock());
		        response.setCreatedAt(product.getCreatedAt());
		        response.setUpdatedAt(product.getUpdatedAt());
		        response.setFinalized(product.isFinalized());
		        response.setCurrentBiddingRound(product.getCurrentBiddingRound());

		        // Fetch image URLs
		        List<ProductImage> images = productImageRepository.findByProductId(product.getId());
		        List<String> imageUrls = images.stream()
		                                       .map(ProductImage::getImageUrl)
		                                       .collect(Collectors.toList());
		        response.setImageUrls(imageUrls);

		        responseList.add(response);
		    }

		    return ResponseEntity.ok(responseList);
		}

	
	 @Transactional
	 public ResponseEntity<?> updateProduct(int productId, ProductUpdateRequest request) {
	     if (bidService.isBiddingLiveNow()) {
	         return ResponseEntity.badRequest().body("Cannot update product while bidding is live.");
	     }

	     Optional<Product> optional = productRepository.findById(productId);
	     if (!optional.isPresent()) {
	         return ResponseEntity.badRequest().body("Product not found with ID: " + productId);
	     }

	     Product product = optional.get();
	     boolean updated = false;

	     if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
	         product.setDescription(request.getDescription().trim());
	         updated = true;
	     }

	     if (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) > 0) {
	         product.setPrice(request.getPrice());
	         updated = true;
	     }

	     if (request.getStock() != null && request.getStock() > 0) {
	         product.setStock(request.getStock());
	         updated = true;
	     }

	     if (!updated) {
	         return ResponseEntity.badRequest().body("No valid fields provided to update.");
	     }

	     product.setUpdatedAt(LocalDateTime.now());
	     productRepository.save(product);

	     // 🔔 WebSocket broadcast
	     Map<String, Object> payload = new HashMap<>();
	     payload.put("type", "PRODUCT_UPDATED");
	     payload.put("productId", product.getId());
	     payload.put("productName", product.getName());
	     payload.put("timestamp", product.getUpdatedAt().toString());

	     notificationService.broadcast(payload);

	     System.out.println("🔄 Product updated and broadcasted: " + product.getId());

	     return ResponseEntity.ok("Product updated successfully.");
	 }



	 @Transactional
	 public ResponseEntity<?> deleteProduct(int productId) {
	     try {
	         // Check if product exists
	         Optional<Product> productOpt = productRepository.findById(productId);
	         if (productOpt.isEmpty()) {
	             return ResponseEntity.badRequest().body("Product not found with ID: " + productId);
	         }

	         Product product = productOpt.get();

	         // Prevent deletion if bids exist for the product
	         if (bidRepository.existsByProductId(productId)) {
	             return ResponseEntity.status(HttpStatus.FORBIDDEN)
	                                  .body("Cannot delete product after bids are placed.");
	         }

	         // Delete all associated product images
	         productImageRepository.deleteByProductId(productId);

	         // Delete the product itself
	         productRepository.deleteById(productId);

	         // 🔔 WebSocket broadcast notification
	         Map<String, Object> payload = new HashMap<>();
	         payload.put("type", "PRODUCT_DELETED");
	         payload.put("productId", product.getId());
	         payload.put("productName", product.getName());
	         payload.put("timestamp", LocalDateTime.now().toString());

	         notificationService.broadcast(payload);

	         System.out.println("🗑️ Product deleted and broadcasted: " + product.getId());

	         return ResponseEntity.ok("Product deleted successfully.");
	     } catch (DataAccessException dae) {
	         dae.printStackTrace();
	         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                              .body("Database error occurred while deleting product.");
	     } catch (Exception e) {
	         e.printStackTrace();
	         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                              .body("An unexpected error occurred: " + e.getMessage());
	     }
	 }




	 public ResponseEntity<?> getProductsBySellerId(int sellerId) {
		    List<Product> products = productRepository.findBySellerId(sellerId);

		    if (products.isEmpty()) {
		        // Optional: notify that no products were found for this seller
		        Map<String, Object> emptyPayload = new HashMap<>();
		        emptyPayload.put("type", "NO_PRODUCTS_FOUND");
		        emptyPayload.put("sellerId", sellerId);
		        notificationService.notifyTopic("products", emptyPayload);

		        return ResponseEntity.ok(Collections.emptyList());
		    }

		    List<ProductResponse> responseList = new ArrayList<>();

		    for (Product product : products) {
		        ProductResponse response = new ProductResponse();
		        response.setId(product.getId());
		        response.setSeller(product.getSeller().getId()); // ✅ fix here
		        response.setName(product.getName());
		        response.setDescription(product.getDescription());
		        response.setCategory(product.getCategory());
		        response.setBrand(product.getBrand());
		        response.setPrice(product.getPrice());
		        response.setMinBidIncrement(product.getMinBidIncrement());
		        response.setProductStatus(product.getProductStatus());
		        response.setStock(product.getStock());
		        response.setCreatedAt(product.getCreatedAt());
		        response.setUpdatedAt(product.getUpdatedAt());

		        List<ProductImage> images = productImageRepository.findByProductId(product.getId());
		        List<String> imageUrls = images.stream()
		                .map(ProductImage::getImageUrl)
		                .collect(Collectors.toList());
		        response.setImageUrls(imageUrls);

		        responseList.add(response);
		    }

		    // ✅ Send WebSocket notification for successful fetch
		    Map<String, Object> payload = new HashMap<>();
		    payload.put("type", "SELLER_PRODUCTS_FETCHED");
		    payload.put("sellerId", sellerId);
		    payload.put("productCount", responseList.size());

		    notificationService.notifyTopic("products", payload);

		    return ResponseEntity.ok(responseList);
		}
	 
	 public ResponseEntity<?> getActiveProducts() {
		    List<Product> activeProducts = productRepository.findByProductStatus(ProductStatus.ACTIVE);

		    // Build response using a DTO-style structure
		    Map<String, Object> response = new HashMap<>();
		    response.put("type", "ACTIVE_PRODUCTS_LIST");
		    response.put("timestamp", LocalDateTime.now().toString());
		    response.put("count", activeProducts.size());
		    response.put("products", activeProducts);

		    return ResponseEntity.ok(response);
		}



	 public ResponseEntity<?> getProductsGroupedByStatus() {
		    List<Product> activeProducts = productRepository.findByProductStatus(ProductStatus.ACTIVE);
		    List<Product> inactiveProducts = productRepository.findByProductStatus(ProductStatus.INACTIVE);

		    // Convert activeProducts to response DTOs with imageUrls
		    List<Map<String, Object>> activeProductResponses = activeProducts.stream().map(p -> {
		        Map<String, Object> map = new HashMap<>();
		        map.put("id", p.getId());
		        map.put("name", p.getName());
		        map.put("description", p.getDescription());
		        map.put("price", p.getPrice());
		        map.put("stock", p.getStock());
		        map.put("category", p.getCategory());
		        map.put("minBidIncrement", p.getMinBidIncrement());
		        map.put("status", p.getProductStatus());

		        List<ProductImage> images = productImageRepository.findByProductId(p.getId());
		        List<String> imageUrls = images.stream()
		                .map(ProductImage::getImageUrl)
		                .collect(Collectors.toList());
		        map.put("imageUrls", imageUrls);

		        return map;
		    }).toList();

		    // Convert inactiveProducts to response DTOs with imageUrls
		    List<Map<String, Object>> inactiveProductResponses = inactiveProducts.stream().map(p -> {
		        Map<String, Object> map = new HashMap<>();
		        map.put("id", p.getId());
		        map.put("name", p.getName());
		        map.put("description", p.getDescription());
		        map.put("price", p.getPrice());
		        map.put("stock", p.getStock());
		        map.put("category", p.getCategory());
		        map.put("minBidIncrement", p.getMinBidIncrement());
		        map.put("status", p.getProductStatus());

		        List<ProductImage> images = productImageRepository.findByProductId(p.getId());
		        List<String> imageUrls = images.stream()
		                .map(ProductImage::getImageUrl)
		                .collect(Collectors.toList());
		        map.put("imageUrls", imageUrls);

		        return map;
		    }).toList();

		    // Final HTTP response body
		    Map<String, Object> response = new HashMap<>();
		    response.put("activeProducts", activeProductResponses);
		    response.put("inactiveProducts", inactiveProductResponses);

		    // 🔔 1. WebSocket broadcast: PRODUCT_STATUS_SUMMARY (always sent)
		    Map<String, Object> summaryPayload = new HashMap<>();
		    summaryPayload.put("type", "PRODUCT_STATUS_SUMMARY");
		    summaryPayload.put("timestamp", LocalDateTime.now().toString());
		    summaryPayload.put("activeCount", activeProducts.size());
		    summaryPayload.put("inactiveCount", inactiveProducts.size());

		    notificationService.broadcastToTopic("/topic/products", summaryPayload);

		    // 🔍 2. WebSocket only for recently activated products
		    LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
		    List<Product> recentlyActivated = activeProducts.stream()
		            .filter(p -> p.getUpdatedAt() != null && p.getUpdatedAt().isAfter(fiveMinutesAgo))
		            .toList();

		    if (!recentlyActivated.isEmpty()) {
		        List<Map<String, Object>> activatedPayloadList = recentlyActivated.stream().map(p -> {
		            Map<String, Object> map = new HashMap<>();
		            map.put("id", p.getId());
		            map.put("name", p.getName());
		            map.put("price", p.getPrice());
		            map.put("status", p.getProductStatus());

		            List<ProductImage> images = productImageRepository.findByProductId(p.getId());
		            List<String> imageUrls = images.stream()
		                    .map(ProductImage::getImageUrl)
		                    .collect(Collectors.toList());
		            map.put("imageUrls", imageUrls);

		            return map;
		        }).toList();

		        Map<String, Object> activationPayload = new HashMap<>();
		        activationPayload.put("type", "NEWLY_ACTIVATED_PRODUCTS");
		        activationPayload.put("timestamp", LocalDateTime.now().toString());
		        activationPayload.put("products", activatedPayloadList);

		        notificationService.broadcastToTopic("/topic/products", activationPayload);
		    }

		    return ResponseEntity.ok(response);
		}



	   

	    public boolean isProductBiddable(Product product) {
	        if (product.getProductStatus() != ProductStatus.ACTIVE) {
	            return false; // Sold or inactive
	        }

	        LocalTime nowTime = LocalTime.now();
	        LocalDate today = LocalDate.now();

	        // Check daily config
	        DailyBiddingTimeConfig config = dailyConfigRepository.findById(1).orElse(null);
	        boolean withinDailyWindow = false;
	        if (config != null) {
	            LocalTime start = config.getDailyStartTime();
	            LocalTime end = config.getDailyEndTime();

	            if (start.isBefore(end)) {
	                // Normal window e.g. 09:00 to 17:00
	                withinDailyWindow = !nowTime.isBefore(start) && !nowTime.isAfter(end);
	            } else {
	                // Overnight window e.g. 22:00 to 02:00
	                withinDailyWindow = !nowTime.isBefore(start) || !nowTime.isAfter(end);
	            }
	        }

	        // Check time slots for today
	        List<BiddingTimeSlot> slots = slotRepository.findBySlotDate(today);
	        boolean withinSlotWindow = slots.stream().anyMatch(slot -> {
	            LocalTime slotStart = slot.getSlotStartTime();
	            LocalTime slotEnd = slot.getSlotEndTime();

	            if (slotStart.isBefore(slotEnd)) {
	                return !nowTime.isBefore(slotStart) && !nowTime.isAfter(slotEnd);
	            } else {
	                // Overnight slot window
	                return !nowTime.isBefore(slotStart) || !nowTime.isAfter(slotEnd);
	            }
	        });

	        return withinDailyWindow || withinSlotWindow;
	    }

	    @Scheduled(fixedRate = 60000)
	    @Transactional
	    public void checkAndFinalizeExpiredBids() {
	        LocalDateTime now = LocalDateTime.now();
	        LocalTime nowTime = now.toLocalTime();
	        LocalDate today = now.toLocalDate();

	        DailyBiddingTimeConfig config = dailyConfigRepository.findById(1).orElse(null);
	        if (config == null) return;

	        LocalTime start = config.getDailyStartTime();
	        LocalTime end = config.getDailyEndTime();

	        boolean withinDailyWindow = start.isBefore(end)
	            ? !nowTime.isBefore(start) && !nowTime.isAfter(end)
	            : !nowTime.isBefore(start) || !nowTime.isAfter(end);

	        List<BiddingTimeSlot> slots = slotRepository.findBySlotDate(today);
	        boolean withinSlotWindow = slots.stream().anyMatch(slot -> {
	            LocalTime slotStart = slot.getSlotStartTime();
	            LocalTime slotEnd = slot.getSlotEndTime();
	            return slotStart.isBefore(slotEnd)
	                ? !nowTime.isBefore(slotStart) && !nowTime.isAfter(slotEnd)
	                : !nowTime.isBefore(slotStart) || !nowTime.isAfter(slotEnd);
	        });

	        if (!withinDailyWindow && !withinSlotWindow) return;

	        List<Product> activeProducts = productRepository.findByProductStatus(ProductStatus.ACTIVE);

	        for (Product product : activeProducts) {
	            if (shouldActivateProduct(product, now, start, end, slots)) {
	                product.setLastActivatedAt(now);
	                productRepository.save(product);

	                // 🔔 WebSocket: Notify product is now biddable
	                Map<String, Object> payload = new HashMap<>();
	                payload.put("type", "PRODUCT_ACTIVATED");
	                payload.put("productId", product.getId());
	                payload.put("name", product.getName());
	                payload.put("price", product.getPrice());
	                payload.put("stock", product.getStock());
	                payload.put("timestamp", now.toString());

	                notificationService.notifyTopic("products", payload);
	            }

	            // TODO: Add finalization logic if needed
	        }
	    }
	    
	    private boolean shouldActivateProduct(Product product, LocalDateTime now, LocalTime start, LocalTime end, List<BiddingTimeSlot> slots) {
	        LocalDateTime lastActivated = product.getLastActivatedAt();
	        if (lastActivated == null) return true;

	        // If already activated in this window, skip
	        boolean dailyWindowActive = isWithinTimeWindow(now.toLocalTime(), start, end);
	        boolean slotWindowActive = slots.stream().anyMatch(slot ->
	        isWithinTimeWindow(now.toLocalTime(), slot.getSlotStartTime(), slot.getSlotEndTime()));

	        if (!dailyWindowActive && !slotWindowActive) {
	            return false;
	        }

	        // Check if the last activation was within this same window
	        if (dailyWindowActive) {
	            return !isWithinTimeWindow(lastActivated.toLocalTime(), start, end);
	        }

	        if (slotWindowActive) {
	            for (BiddingTimeSlot slot : slots) {
	                if (isWithinTimeWindow(now.toLocalTime(), slot.getSlotStartTime(), slot.getSlotEndTime()) &&
	                		isWithinTimeWindow(lastActivated.toLocalTime(), slot.getSlotStartTime(), slot.getSlotEndTime())) {
	                    return false;
	                }
	            }
	        }

	        return true;
	    }
	    
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
	    
	    
	    
	    public ResponseEntity<?> getProductById(int productId) {
	    	
	    	Optional<Product> products= productRepository.findById(productId);
	    	
	    	if(products.isEmpty()) {
	    	    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not Found");
	    	}
	    	
	    	Product product = products.get();
	    	
	    	 List<ProductResponse> responseList = new ArrayList<>();
//	    	 for (Product product : products) {
	                    ProductResponse response = new ProductResponse();
	                    response.setId(product.getId());
	                    response.setName(product.getName());
	                    response.setDescription(product.getDescription());
	                    response.setPrice(product.getPrice());
	                    response.setStock(product.getStock());
	                    response.setCategory(product.getCategory());
	                    response.setMinBidIncrement(product.getMinBidIncrement());
	                    response.setProductStatus(product.getProductStatus());
	                    List<ProductImage> images = productImageRepository.findByProductId(product.getId());
	    		        List<String> imageUrls = images.stream()
	    		                .map(ProductImage::getImageUrl)
	    		                .collect(Collectors.toList());
	    		        response.setImageUrls(imageUrls);

	    		        responseList.add(response);
	    		        
	    		        
	                    return ResponseEntity.ok(response);
	                
	               
	    }


}