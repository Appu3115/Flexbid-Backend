package com.example.flexbid.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.flexbid.model.Order;
import com.example.flexbid.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<Payment> findByOrder(Order order);

    boolean existsByRazorpayOrderId(String razorpayOrderId);

	Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}