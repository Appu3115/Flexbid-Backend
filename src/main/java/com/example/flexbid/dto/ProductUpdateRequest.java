package com.example.flexbid.dto;

import java.math.BigDecimal;

public class ProductUpdateRequest {
    private BigDecimal price;
    private Integer stock;
    private String description;
	public BigDecimal getPrice() {
		return price;
	}
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	public Integer getStock() {
		return stock;
	}
	public void setStock(Integer stock) {
		this.stock = stock;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

    // Getters and Setters
}
