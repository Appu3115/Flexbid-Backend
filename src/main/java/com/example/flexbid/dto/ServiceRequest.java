package com.example.flexbid.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ServiceRequest {

	private String title;
    private String description;
    private String category;
    private BigDecimal maxBudget;
    private int durationDays;
    private int durationHours;
    private int durationMinutes;
    private LocalDate scheduleDate;
    private String location;
	public LocalDate getScheduleDate() {
		return scheduleDate;
	}
	public void setScheduleDate(LocalDate scheduleDate) {
		this.scheduleDate = scheduleDate;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
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
	public int getDurationDays() {
		return durationDays;
	}
	public void setDurationDays(int durationDays) {
		this.durationDays = durationDays;
	}
	public int getDurationHours() {
		return durationHours;
	}
	public void setDurationHours(int durationHours) {
		this.durationHours = durationHours;
	}
	public int getDurationMinutes() {
		return durationMinutes;
	}
	public void setDurationMinutes(int durationMinutes) {
		this.durationMinutes = durationMinutes;
	}
}
