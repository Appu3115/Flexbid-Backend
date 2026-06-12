package com.example.flexbid.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidResponseDTO {
    private int bidId;
    private int buyerId;
    private String buyerName;
    private int productId;
    private String productName;
    private BigDecimal amount;
    private boolean bidIncrement;
    private LocalDateTime bidTime;
    private boolean withdrawn;
    private int biddingRound;

    public int getBiddingRound() {
		return biddingRound;
	}

	public void setBiddingRound(int biddingRound) {
		this.biddingRound = biddingRound;
	}

	public boolean isWithdrawn() {
		return withdrawn;
	}

	public void setWithdrawn(boolean withdrawn) {
		this.withdrawn = withdrawn;
	}

	public int getBidId() {
		return bidId;
	}

	public void setBidId(int bidId) {
		this.bidId = bidId;
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

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public boolean isBidIncrement() {
		return bidIncrement;
	}

	public void setBidIncrement(boolean bidIncrement) {
		this.bidIncrement = bidIncrement;
	}

	public LocalDateTime getBidTime() {
		return bidTime;
	}

	public void setBidTime(LocalDateTime bidTime) {
		this.bidTime = bidTime;
	}

	// Constructors
    public BidResponseDTO(int bidId, int buyerId, String buyerName,
                          int productId, String productName,
                          BigDecimal amount, boolean bidIncrement, LocalDateTime bidTime,boolean withdrawn,int biddingRound) {
        this.bidId = bidId;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.productId = productId;
        this.productName = productName;
        this.amount = amount;
        this.bidIncrement = bidIncrement;
        this.bidTime = bidTime;
        this.withdrawn=withdrawn;
        this.biddingRound = biddingRound;
    }


    // Getters & setters (or use Lombok if allowed)
}

