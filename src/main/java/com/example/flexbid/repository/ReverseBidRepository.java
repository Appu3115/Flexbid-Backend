package com.example.flexbid.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.flexbid.model.ReverseBid;

public interface ReverseBidRepository extends JpaRepository<ReverseBid, Integer> {
    List<ReverseBid> findByRequestId(int requestId);
    List<ReverseBid> findBySellerId(int sellerId);
	List<ReverseBid> findByRequestIdOrderByBidAmountAsc(int id);
	Optional<ReverseBid> findFirstByRequestIdAndIsWinnerTrue(int requestId);
	ReverseBid findTopByRequestIdOrderByBidAmountAsc(int requestId);
	ReverseBid findTopByRequestIdAndSellerIdOrderByCreatedAtDesc(int requestId, int sellerId);
	ReverseBid findTopByRequestIdOrderByCreatedAtDesc(int requestId);
	List<ReverseBid> findByRequestIdOrderByCreatedAtDesc(int requestId);
	List<ReverseBid> findBySellerIdAndIsWinnerTrue(int sellerId);
}

