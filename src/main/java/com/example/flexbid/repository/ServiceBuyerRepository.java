package com.example.flexbid.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.flexbid.model.Buyer;


@Repository
public interface ServiceBuyerRepository extends JpaRepository<Buyer, Integer> {
    List<Buyer> findByBuyerId(int buyerId);

	List<Buyer> findByStatusAndBiddingEndBefore(String string, LocalDateTime now);

	void deleteByBuyerId(Integer userId);

	
}
