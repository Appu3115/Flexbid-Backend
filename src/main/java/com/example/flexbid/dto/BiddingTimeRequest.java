package com.example.flexbid.dto;

//import java.time.LocalTime;

public class BiddingTimeRequest {
	private String startTime;
    private String endTime;

    // Getters and Setters
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
