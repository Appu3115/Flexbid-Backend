package com.example.flexbid.controller;

//import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;

import com.example.flexbid.service.SellerPortfolioService;

@RestController
@RequestMapping("/api/seller")
@CrossOrigin(origins="*")
public class SellerPortfolioController {

    @Autowired
    private SellerPortfolioService portfolioService;

    // 🔼 Upload work files (photo/video/doc)
    @PostMapping("/{sellerId}/add-link")
    public ResponseEntity<?> addPortfolioItem(
            @PathVariable Integer sellerId,
            @RequestParam String url,
            @RequestParam String type,
            @RequestParam String title) {
        return portfolioService.savePortfolioLink(sellerId, url, type, title);
    }

    @GetMapping("/{sellerId}/portfolio")
    public ResponseEntity<?> getPortfolio(@PathVariable Integer sellerId) {
        return portfolioService.getPortfolioItems(sellerId);
    }
}