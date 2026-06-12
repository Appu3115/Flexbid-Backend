package com.example.flexbid.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "daily_bidding_time_config")
public class DailyBiddingTimeConfig {
    @Id
    private int id = 1; // only one row

    private LocalTime dailyStartTime;
    private LocalTime dailyEndTime;
    private LocalDateTime updatedAt;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public LocalTime getDailyStartTime() {
		return dailyStartTime;
	}
	public void setDailyStartTime(LocalTime dailyStartTime) {
		this.dailyStartTime = dailyStartTime;
	}
	public LocalTime getDailyEndTime() {
		return dailyEndTime;
	}
	public void setDailyEndTime(LocalTime dailyEndTime) {
		this.dailyEndTime = dailyEndTime;
	}
	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

}

