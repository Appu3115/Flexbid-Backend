package com.example.flexbid.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

public class BiddingSlotRequest {
	@Schema(description = "Date of the slot (leave blank to use today)", example = "yyyy-mm-dd")
    private LocalDate slotDate;

    @Schema(description = "Start time in HH:mm:ss format", example = "")
    private String startTime;

    @Schema(description = "End time in HH:mm:ss format", example = "")
    private String endTime;

    // getters/setters
	public LocalDate getSlotDate() {
		return slotDate;
	}
	public void setSlotDate(LocalDate slotDate) {
		this.slotDate = slotDate;
	}
	public String getStartTime() {
		return startTime;
	}
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}
	public String getEndTime() {
		return endTime;
	}
	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}
}
