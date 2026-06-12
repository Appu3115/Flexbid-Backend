package com.example.flexbid.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "service_requests")
public class Buyer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int buyerId;
    private String title;
    private String description;
    private String category;
    private BigDecimal maxBudget;
    private LocalDateTime biddingStart;
    private LocalDateTime biddingEnd;
    private String status = "OPEN";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
	private Integer winnerId;
	private LocalDate scheduleDate;
	private boolean isScheduleConfirmed = false;
	private LocalDate rescheduleDate;
	private int rescheduleCount = 0;
	private boolean reschedulePending = false;      // New: Track if buyer needs to respond
	public boolean isReschedulePending() {
		return reschedulePending;
	}

	public void setReschedulePending(boolean reschedulePending) {
		this.reschedulePending = reschedulePending;
	}

	public boolean isRescheduleAccepted() {
		return rescheduleAccepted;
	}

	public void setRescheduleAccepted(boolean rescheduleAccepted) {
		this.rescheduleAccepted = rescheduleAccepted;
	}

	private boolean rescheduleAccepted = false;     // Optional: Track if last request was accepted
	private String location;
	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public LocalDate getScheduleDate() {
		return scheduleDate;
	}

	public void setScheduleDate(LocalDate scheduleDate) {
		this.scheduleDate = scheduleDate;
	}

	public boolean isScheduleConfirmed() {
		return isScheduleConfirmed;
	}

	public void setScheduleConfirmed(boolean isScheduleConfirmed) {
		this.isScheduleConfirmed = isScheduleConfirmed;
	}

	public LocalDate getRescheduleDate() {
		return rescheduleDate;
	}

	public void setRescheduleDate(LocalDate rescheduleDate) {
		this.rescheduleDate = rescheduleDate;
	}

	public int getRescheduleCount() {
		return rescheduleCount;
	}

	public void setRescheduleCount(int rescheduleCount) {
		this.rescheduleCount = rescheduleCount;
	}

	public Integer getWinnerId() {
		return winnerId;
	}

	public void setWinnerId(Integer winnerId) {
		this.winnerId = winnerId;
	}

	private LocalDateTime updatedAt;
	
	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
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

	public BigDecimal getMaxBudget() {
		return maxBudget;
	}

	public void setMaxBudget(BigDecimal maxBudget) {
		this.maxBudget = maxBudget;
	}

	public LocalDateTime getBiddingStart() {
		return biddingStart;
	}

	public void setBiddingStart(LocalDateTime biddingStart) {
		this.biddingStart = biddingStart;
	}

	public LocalDateTime getBiddingEnd() {
		return biddingEnd;
	}

	public void setBiddingEnd(LocalDateTime biddingEnd) {
		this.biddingEnd = biddingEnd;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

    // Getters and setters
}

