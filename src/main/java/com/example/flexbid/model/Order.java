package com.example.flexbid.model;

import java.math.BigDecimal;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // User placing the order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Winning bid linked to this order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id", nullable = false)
    private Bid bid;

    // Product linked to this order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity = 1;  // primitive int matches Product stock and Bid quantity

    @Column(name = "billing_name", length = 255)
    private String billingName;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Column(name = "payment_method", length = 100)
    private String paymentMethod;

    // Use Integer or BigDecimal for totalAmount? Your Product price is BigDecimal,
    // but your current Order uses Integer. Usually for amounts use BigDecimal.
    // Let's update to BigDecimal to be consistent with Product and Bid amounts.
    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 20)
    private DeliveryStatus deliveryStatus = DeliveryStatus.PLACED;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_status", length = 20)
    private RequestStatus cancelStatus = RequestStatus.NOT_REQUESTED;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "cancel_requested_at")
    private LocalDateTime cancelRequestedAt;

    @Column(name = "cancel_completed_at")
    private LocalDateTime cancelCompletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_status", length = 20)
    private RequestStatus returnStatus = RequestStatus.NOT_REQUESTED;

    @Column(name = "return_reason", columnDefinition = "TEXT")
    private String returnReason;

    @Column(name = "return_requested_at")
    private LocalDateTime returnRequestedAt;

    @Column(name = "return_completed_at")
    private LocalDateTime returnCompletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "replacement_status", length = 20)
    private RequestStatus replacementStatus = RequestStatus.NOT_REQUESTED;

    @Column(name = "replacement_reason", columnDefinition = "TEXT")
    private String replacementReason;

    @Column(name = "replacement_requested_at")
    private LocalDateTime replacementRequestedAt;

    @Column(name = "replacement_completed_at")
    private LocalDateTime replacementCompletedAt;

    @Column(name = "pickup_status", length = 100)
    private String pickupStatus;

    @Column(name = "refund_initiated")
    private Boolean refundInitiated = false;

    @Column(name = "refund_completed")
    private Boolean refundCompleted = false;

    @Column(name = "refund_initiated_at")
    private LocalDateTime refundInitiatedAt;

    @Column(name = "refund_completed_at")
    private LocalDateTime refundCompletedAt;

    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason;

    @Column(name = "razorpay_refund_id", length = 255)
    private String razorpayRefundId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and setters ...



	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
	 public User getUser() {
			return user;
		}

		public void setUser(User user) {
			this.user = user;
		}

		public Bid getBid() {
			return bid;
		}

		public void setBid(Bid bid) {
			this.bid = bid;
		}

		public Product getProduct() {
			return product;
		}

		public void setProduct(Product product) {
			this.product = product;
		}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public String getBillingName() {
		return billingName;
	}

	public void setBillingName(String billingName) {
		this.billingName = billingName;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getContactNumber() {
		return contactNumber;
	}

	public void setContactNumber(String contactNumber) {
		this.contactNumber = contactNumber;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	public DeliveryStatus getDeliveryStatus() {
		return deliveryStatus;
	}

	public void setDeliveryStatus(DeliveryStatus deliveryStatus) {
		this.deliveryStatus = deliveryStatus;
	}

	public RequestStatus getCancelStatus() {
		return cancelStatus;
	}

	public void setCancelStatus(RequestStatus cancelStatus) {
		this.cancelStatus = cancelStatus;
	}

	public String getCancelReason() {
		return cancelReason;
	}

	public void setCancelReason(String cancelReason) {
		this.cancelReason = cancelReason;
	}

	public LocalDateTime getCancelRequestedAt() {
		return cancelRequestedAt;
	}

	public void setCancelRequestedAt(LocalDateTime cancelRequestedAt) {
		this.cancelRequestedAt = cancelRequestedAt;
	}

	public LocalDateTime getCancelCompletedAt() {
		return cancelCompletedAt;
	}

	public void setCancelCompletedAt(LocalDateTime cancelCompletedAt) {
		this.cancelCompletedAt = cancelCompletedAt;
	}

	public RequestStatus getReturnStatus() {
		return returnStatus;
	}

	public void setReturnStatus(RequestStatus returnStatus) {
		this.returnStatus = returnStatus;
	}

	public String getReturnReason() {
		return returnReason;
	}

	public void setReturnReason(String returnReason) {
		this.returnReason = returnReason;
	}

	public LocalDateTime getReturnRequestedAt() {
		return returnRequestedAt;
	}

	public void setReturnRequestedAt(LocalDateTime returnRequestedAt) {
		this.returnRequestedAt = returnRequestedAt;
	}

	public LocalDateTime getReturnCompletedAt() {
		return returnCompletedAt;
	}

	public void setReturnCompletedAt(LocalDateTime returnCompletedAt) {
		this.returnCompletedAt = returnCompletedAt;
	}

	public RequestStatus getReplacementStatus() {
		return replacementStatus;
	}

	public void setReplacementStatus(RequestStatus replacementStatus) {
		this.replacementStatus = replacementStatus;
	}

	public String getReplacementReason() {
		return replacementReason;
	}

	public void setReplacementReason(String replacementReason) {
		this.replacementReason = replacementReason;
	}

	public LocalDateTime getReplacementRequestedAt() {
		return replacementRequestedAt;
	}

	public void setReplacementRequestedAt(LocalDateTime replacementRequestedAt) {
		this.replacementRequestedAt = replacementRequestedAt;
	}

	public LocalDateTime getReplacementCompletedAt() {
		return replacementCompletedAt;
	}

	public void setReplacementCompletedAt(LocalDateTime replacementCompletedAt) {
		this.replacementCompletedAt = replacementCompletedAt;
	}

	public String getPickupStatus() {
		return pickupStatus;
	}

	public void setPickupStatus(String pickupStatus) {
		this.pickupStatus = pickupStatus;
	}

	public Boolean getRefundInitiated() {
		return refundInitiated;
	}

	public void setRefundInitiated(Boolean refundInitiated) {
		this.refundInitiated = refundInitiated;
	}

	public Boolean getRefundCompleted() {
		return refundCompleted;
	}

	public void setRefundCompleted(Boolean refundCompleted) {
		this.refundCompleted = refundCompleted;
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

    // getters and setters
}


