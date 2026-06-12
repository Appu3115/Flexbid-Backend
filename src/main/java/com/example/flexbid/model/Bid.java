package com.example.flexbid.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "bids")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "buyer_id", nullable = false)
    private int buyerId;

    @Column(name = "product_id", nullable = false)
    private int productId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "bid_increment", nullable = false)
    private boolean BidIncrement;

    @Column(name = "bid_time", nullable = false)
    private LocalDateTime bidTime;

    @Transient
    private Product product;

    @Transient
    private User buyer;

    @Column(name = "left_bid", nullable = false)
    private boolean leftBid = false;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "winning_bid", nullable = false)
    private boolean winningBid = false;

    @Column(name = "bidding_round", nullable = false)
    private int biddingRound = 1;

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(int buyerId) {
        this.buyerId = buyerId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public boolean isBidIncrement() {
        return BidIncrement;
    }

    public void setBidIncrement(boolean usedMinBidIncrement) {
        this.BidIncrement = usedMinBidIncrement;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public boolean isWinningBid() {
        return winningBid;
    }

    public void setWinningBid(boolean winningBid) {
        this.winningBid = winningBid;
    }

    public User getBuyer() {
        return buyer;
    }

    public void setBuyer(User buyer) {
        this.buyer = buyer;
    }

    public boolean isLeftBid() {
        return leftBid;
    }

    public void setLeftBid(boolean leftBid) {
        this.leftBid = leftBid;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getBiddingRound() {
        return biddingRound;
    }

    public void setBiddingRound(int biddingRound) {
        this.biddingRound = biddingRound;
    }
}