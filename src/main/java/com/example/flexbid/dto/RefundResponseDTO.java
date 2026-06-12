package com.example.flexbid.dto;

public class RefundResponseDTO {
    private String message;
    private boolean refundInitiated;
    private String razorpayRefundId;
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public boolean isRefundInitiated() {
		return refundInitiated;
	}
	public void setRefundInitiated(boolean refundInitiated) {
		this.refundInitiated = refundInitiated;
	}
	public String getRazorpayRefundId() {
		return razorpayRefundId;
	}
	public void setRazorpayRefundId(String razorpayRefundId) {
		this.razorpayRefundId = razorpayRefundId;
	}
}