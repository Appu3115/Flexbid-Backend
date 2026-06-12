package com.example.flexbid.service;

import java.util.*;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.flexbid.model.SellerPortfolioItem;
import com.example.flexbid.model.SellerProfile;
import com.example.flexbid.model.User;
import com.example.flexbid.repository.SellerPortfolioItemRepository;
import com.example.flexbid.repository.SellerProfileRepository;
import com.example.flexbid.repository.UserRepository;

@Service
public class SellerPortfolioService {

    private static final Set<String> ALLOWED_TYPES = Set.of("photo", "video", "doc", "link");

    private static final Pattern URL_PATTERN = Pattern.compile("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$");

    @Autowired
    private SellerProfileRepository sellerProfileRepo;

    @Autowired
    private SellerPortfolioItemRepository portfolioItemRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private WebSocketNotificationService notificationService;

    // ✅ Save any external URL (Cloudinary, YouTube, GitHub, etc.)
    public ResponseEntity<?> savePortfolioLink(Integer sellerId, String url, String type, String title) {
        try {
            if (!ALLOWED_TYPES.contains(type)) {
                return ResponseEntity.badRequest().body("Invalid type. Allowed types: photo, video, doc, link");
            }

            if (url == null || !URL_PATTERN.matcher(url).matches()) {
                return ResponseEntity.badRequest().body("Invalid URL format");
            }

            Optional<User> optionalUser = userRepo.findById(sellerId);
            if (optionalUser.isEmpty()) {
                return ResponseEntity.badRequest().body("Seller not found");
            }

            User user = optionalUser.get();

            SellerProfile profile = sellerProfileRepo.findByUserId(sellerId)
                    .orElseGet(() -> sellerProfileRepo.save(new SellerProfile(user)));

            SellerPortfolioItem item = new SellerPortfolioItem();
            item.setFileUrl(url);
            item.setType(type);
            item.setTitle(title);
            item.setExternal(true);
            item.setSellerProfile(profile);

            portfolioItemRepo.save(item);

            // 🔔 Notify subscribers via WebSocket
            notificationService.notifyTopic("seller-portfolio/" + sellerId, Map.of(
                "event", "PORTFOLIO_LINK_ADDED",
                "type", type,
                "title", title
            ));

            return ResponseEntity.ok("Portfolio item added successfully");
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Error saving portfolio item: " + ex.getMessage());
        }
    }

    // 📥 Fetch all portfolio items for seller
    public ResponseEntity<?> getPortfolioItems(Integer sellerId) {
        try {
            Optional<User> optionalUser = userRepo.findById(sellerId);
            if (optionalUser.isEmpty()) {
                return ResponseEntity.badRequest().body("Seller not found");
            }

            User user = optionalUser.get();

            SellerProfile profile = sellerProfileRepo.findByUserId(sellerId)
                    .orElseGet(() -> sellerProfileRepo.save(new SellerProfile(user)));

            List<SellerPortfolioItem> items = portfolioItemRepo.findBySellerProfile(profile);
            System.out.println("Portfolio items count: " + items.size());

            return ResponseEntity.ok(items);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Error fetching portfolio: " + ex.getMessage());
        }
    }
}
