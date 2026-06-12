package com.example.flexbid.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.flexbid.model.SellerProfile;

@Repository
public interface SellerProfileRepository extends JpaRepository<SellerProfile, Integer> {
    Optional<SellerProfile> findByUserId(Integer sellerId);
}
