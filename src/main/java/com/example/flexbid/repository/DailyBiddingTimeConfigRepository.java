package com.example.flexbid.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.flexbid.model.DailyBiddingTimeConfig;

@Repository
public interface DailyBiddingTimeConfigRepository extends JpaRepository<DailyBiddingTimeConfig, Integer> {
}
