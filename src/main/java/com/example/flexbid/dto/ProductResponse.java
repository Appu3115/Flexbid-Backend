package com.example.flexbid.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.flexbid.model.ProductStatus;

public class ProductResponse {
    private int id;
    private Integer sellerId;
    private String name;
    private String description;
    private String category;
    private String brand;
    private BigDecimal price;
    private BigDecimal minBidIncrement;
    private ProductStatus productStatus;
	private int stock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> imageUrls;
    
    private boolean finalized;
    private int currentBiddingRound;
	public int getCurrentBiddingRound() {
		return currentBiddingRound;
	}
	public void setCurrentBiddingRound(int currentBiddingRound) {
		this.currentBiddingRound = currentBiddingRound;
	}
	public boolean isFinalized() {
		return finalized;
	}
	public void setFinalized(boolean finalized) {
		this.finalized = finalized;
	}
	public ProductStatus getProductStatus() {
		return productStatus;
	}
	public void setProductStatus(ProductStatus productStatus) {
		this.productStatus = productStatus;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public Integer getSellerId() {
		return sellerId;
	}
	public void setSeller(Integer sellerId) {
		this.sellerId= sellerId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String getBrand() {
		return brand;
	}
	public void setBrand(String brand) {
		this.brand = brand;
	}
	public BigDecimal getPrice() {
		return price;
	}
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	public BigDecimal getMinBidIncrement() {
		return minBidIncrement;
	}
	public void setMinBidIncrement(BigDecimal minBidIncrement) {
		this.minBidIncrement = minBidIncrement;
	}
	public int getStock() {
		return stock;
	}
	public void setStock(int stock) {
		this.stock = stock;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
	public List<String> getImageUrls() {
		return imageUrls;
	}
	public void setImageUrls(List<String> imageUrls) {
		this.imageUrls = imageUrls;
	}

    // Getters and Setters
}
