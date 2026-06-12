package com.example.flexbid.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "seller_id")
    private User seller;

	@Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;
    private String brand;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "min_bid_increment", nullable = false)
    private BigDecimal minBidIncrement;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_status")
    private ProductStatus productStatus = ProductStatus.ACTIVE;

    @Column(nullable = false)
    private int stock;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Column(name = "last_winning_bid_id")
    private Integer lastWinningBidId;

    @Column(nullable = false)
    private boolean finalized = false;

    @Version
    private Integer version;
    
 // Add these new fields:

    @Column(name = "current_price")
    private BigDecimal currentPrice;

    @Column(name = "last_bid_time")
    private LocalDateTime lastBidTime;


    public BigDecimal getCurrentPrice() {
		return currentPrice;
	}

	public void setCurrentPrice(BigDecimal currentPrice) {
		this.currentPrice = currentPrice;
	}

	public LocalDateTime getLastBidTime() {
		return lastBidTime;
	}

	public void setLastBidTime(LocalDateTime lastBidTime) {
		this.lastBidTime = lastBidTime;
	}

	@Column(name ="currentBiddingRound",nullable = false)
    private int currentBiddingRound = 1; // default round starts at 1

    // Getters and Setters
    @Column(name = "last_activated_at")
    private LocalDateTime lastActivatedAt;
    
    @Column(name = "last_rescheduled_at")
    private LocalDateTime lastRescheduledAt;


    public LocalDateTime getLastRescheduledAt() {
		return lastRescheduledAt;
	}

	public void setLastRescheduledAt(LocalDateTime lastRescheduledAt) {
		this.lastRescheduledAt = lastRescheduledAt;
	}

	public LocalDateTime getLastActivatedAt() {
		return lastActivatedAt;
	}

	public void setLastActivatedAt(LocalDateTime lastActivatedAt) {
		this.lastActivatedAt = lastActivatedAt;
	}

	public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getSeller() {
  		return seller;
  	}

  	public void setSeller(User integer) {
  		this.seller = integer;
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

    public ProductStatus getProductStatus() {
        return productStatus;
    }

    public void setProductStatus(ProductStatus productStatus) {
        this.productStatus = productStatus;
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

    public Integer getLastWinningBidId() {
        return lastWinningBidId;
    }

    public void setLastWinningBidId(Integer lastWinningBidId) {
        this.lastWinningBidId = lastWinningBidId;
    }

    public boolean isFinalized() {
        return finalized;
    }

    public void setFinalized(boolean finalized) {
        this.finalized = finalized;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public int getCurrentBiddingRound() {
        return currentBiddingRound;
    }

    public void setCurrentBiddingRound(int currentBiddingRound) {
        this.currentBiddingRound = currentBiddingRound;
    }
}