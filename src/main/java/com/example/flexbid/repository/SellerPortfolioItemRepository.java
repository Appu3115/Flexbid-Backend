package com.example.flexbid.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.flexbid.model.SellerPortfolioItem;
import com.example.flexbid.model.SellerProfile;

@Repository
public interface SellerPortfolioItemRepository extends JpaRepository<SellerPortfolioItem, Integer> {
	List<SellerPortfolioItem> findBySellerProfile(SellerProfile sellerProfile);
}