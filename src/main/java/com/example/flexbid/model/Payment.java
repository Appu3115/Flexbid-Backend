package com.example.flexbid.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status")
    private RefundStatus refundStatus = RefundStatus.NOT_REQUESTED;

    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason;

    @Column(name = "razorpay_refund_id")
    private String razorpayRefundId;

    @Column(name = "refund_requested_at")
    private LocalDateTime refundRequestedAt;

    @Column(name = "refund_initiated_at")
    private LocalDateTime refundInitiatedAt;

    @Column(name = "refund_completed_at")
    private LocalDateTime refundCompletedAt;

    @Column(name = "refund_failed_at")
    private LocalDateTime refundFailedAt;

    @Column(name = "refund_failure_reason", columnDefinition = "TEXT")
    private String refundFailureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public String getRazorpayOrderId() {
		return razorpayOrderId;
	}

	public void setRazorpayOrderId(String razorpayOrderId) {
		this.razorpayOrderId = razorpayOrderId;
	}

	public String getRazorpayPaymentId() {
		return razorpayPaymentId;
	}

	public void setRazorpayPaymentId(String razorpayPaymentId) {
		this.razorpayPaymentId = razorpayPaymentId;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public PaymentStatus getPaymentStatus() {
		return paymentStatus;
	}

	public void setPaymentStatus(PaymentStatus paymentStatus) {
		this.paymentStatus = paymentStatus;
	}

	public RefundStatus getRefundStatus() {
		return refundStatus;
	}

	public void setRefundStatus(RefundStatus refundStatus) {
		this.refundStatus = refundStatus;
	}

	public String getRefundReason() {
		return refundReason;
	}

	public void setRefundReason(String refundReason) {
		this.refundReason = refundReason;
	}

	public String getRazorpayRefundId() {
		return razorpayRefundId;
	}

	public void setRazorpayRefundId(String razorpayRefundId) {
		this.razorpayRefundId = razorpayRefundId;
	}

	public LocalDateTime getRefundRequestedAt() {
		return refundRequestedAt;
	}

	public void setRefundRequestedAt(LocalDateTime refundRequestedAt) {
		this.refundRequestedAt = refundRequestedAt;
	}

	public LocalDateTime getRefundInitiatedAt() {
		return refundInitiatedAt;
	}

	public void setRefundInitiatedAt(LocalDateTime refundInitiatedAt) {
		this.refundInitiatedAt = refundInitiatedAt;
	}

	public LocalDateTime getRefundCompletedAt() {
		return refundCompletedAt;
	}

	public void setRefundCompletedAt(LocalDateTime refundCompletedAt) {
		this.refundCompletedAt = refundCompletedAt;
	}

	public LocalDateTime getRefundFailedAt() {
		return refundFailedAt;
	}

	public void setRefundFailedAt(LocalDateTime refundFailedAt) {
		this.refundFailedAt = refundFailedAt;
	}

	public String getRefundFailureReason() {
		return refundFailureReason;
	}

	public void setRefundFailureReason(String refundFailureReason) {
		this.refundFailureReason = refundFailureReason;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

    // Getters and setters omitted for brevity
}