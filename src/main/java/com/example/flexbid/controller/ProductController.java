package com.example.flexbid.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.flexbid.dto.ProductRequest;
import com.example.flexbid.dto.ProductUpdateRequest;
import com.example.flexbid.service.ProductService;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

 // Add new product (seller only)
    @PostMapping("/add")
    public ResponseEntity<?> addProduct(@RequestBody ProductRequest request) {
        return productService.addProduct(request);
    }

    // Update product (seller only)
    @PutMapping("/update/{productId}")
    public ResponseEntity<?> updateProduct(@PathVariable int productId, @RequestBody ProductUpdateRequest request) {
        return productService.updateProduct(productId, request);
    }

    // Delete product (seller only)
    @DeleteMapping("/delete/{productId}")
    public ResponseEntity<?> deleteProduct(@PathVariable int productId) {
        return productService.deleteProduct(productId);
    }

    // Get all products by seller
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<?> getProductsBySeller(@PathVariable int sellerId) {
        return productService.getProductsBySellerId(sellerId);
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveProducts(){
    	return productService.getActiveProducts();
    }
    // Get all active products for bidding (buyer)
    @GetMapping("/active/Inactive")
    public ResponseEntity<?> getActiveAndInactiveProducts() {
        return productService.getProductsGroupedByStatus();
    }
    
    @GetMapping("/all")
    public ResponseEntity<?> getAllProductsWithImages() {
        return productService.getAllProductsWithImages();
    }
    
    @GetMapping("/{productId}")
    public ResponseEntity<?> getProductById(@PathVariable int productId) {
        return productService.getProductById(productId);
    }

}