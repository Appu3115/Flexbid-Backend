package com.example.flexbid.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.flexbid.model.Product;
import com.example.flexbid.model.ProductStatus;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findBySellerId(Integer sellerId);

	List<Product> findByProductStatus(ProductStatus active);

	List<Product> findByProductStatusAndFinalized(ProductStatus inactive, boolean b);

	List<Product> findByStockGreaterThan(int i);

	

}

