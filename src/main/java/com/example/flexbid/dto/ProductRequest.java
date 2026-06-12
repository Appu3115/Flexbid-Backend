package com.example.flexbid.dto;

import java.math.BigDecimal;
import java.util.List;


public class ProductRequest {
	private Integer sellerId;
    private String name;
    private String description;
    private String category;
    private String brand;
    private BigDecimal price;
    private BigDecimal minBidIncrement;
    private int stock;
    private List<String> imageUrls;


	public Integer getSellerId() {
		return sellerId;
	}
	public void setSellerId(Integer sellerId) {
		this.sellerId = sellerId;
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
	public List<String> getImageUrls() {
		return imageUrls;
	}
	public void setImageUrls(List<String> imageUrls) {
		this.imageUrls = imageUrls;
	}

    // Getters and Setters
}

