package com.example.flexbid.dto;

import com.example.flexbid.model.BiddingTimeSlot;

public class BiddingTimeSlotDTO {

    private int id;
    private String startTime;
    private String endTime;
    private String slotDate;

    public String getSlotDate() {
		return slotDate;
	}

	public void setSlotDate(String slotDate) {
		this.slotDate = slotDate;
	}

	public BiddingTimeSlotDTO() {
    }

    public BiddingTimeSlotDTO(int id, String startTime, String endTime,String slotDate) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotDate = slotDate;
    }

    public static BiddingTimeSlotDTO fromEntity(BiddingTimeSlot slot) {
        return new BiddingTimeSlotDTO(
            slot.getId(),
            slot.getSlotStartTime() != null ? slot.getSlotStartTime().toString() : null,
            slot.getSlotEndTime() != null ? slot.getSlotEndTime().toString() : null,
            slot.getSlotDate() != null ? slot.getSlotDate().toString() : null
        );
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
