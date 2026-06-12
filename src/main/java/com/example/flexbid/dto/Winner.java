package com.example.flexbid.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Winner {
    private int productId;
    private String productName;
    private int biddingRound;
    private int buyerId;
    private String buyerName;
    private Long buyerContact;
    private BigDecimal amount;
    private LocalDateTime bidTime;

    public int getProductId() {
		return productId;
	}

	public void setProductId(int productId) {
		this.productId = productId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public int getBiddingRound() {
		return biddingRound;
	}

	public void setBiddingRound(int biddingRound) {
		this.biddingRound = biddingRound;
	}

	public int getBuyerId() {
		return buyerId;
	}

	public void setBuyerId(int buyerId) {
		this.buyerId = buyerId;
	}

	public String getBuyerName() {
		return buyerName;
	}

	public void setBuyerName(String buyerName) {
		this.buyerName = buyerName;
	}

	public Long getBuyerContact() {
		return buyerContact;
	}

	public void setBuyerContact(Long buyerContact2) {
		this.buyerContact = buyerContact2;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public LocalDateTime getBidTime() {
		return bidTime;
	}

	public void setBidTime(LocalDateTime bidTime) {
		this.bidTime = bidTime;
	}

	public Winner() {}

    public Winner(int productId, String productName, int biddingRound, int buyerId,
                  String buyerName, Long buyerContact,
                  BigDecimal amount, LocalDateTime bidTime) {
        this.productId = productId;
        this.productName = productName;
        this.biddingRound = biddingRound;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.buyerContact = buyerContact;
        this.amount = amount;
        this.bidTime = bidTime;
    }
}

