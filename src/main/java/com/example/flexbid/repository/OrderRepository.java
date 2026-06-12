package com.example.flexbid.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.flexbid.model.Bid;
import com.example.flexbid.model.Order;
import com.example.flexbid.model.OrderStatus;
import com.example.flexbid.model.User;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByUser(User user);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByUserId(Integer userId);

    List<Order> findByProductId(Integer productId);

    boolean existsByBidId(Integer bidId);

	boolean existsByBid(Bid bid);

	void deleteByUserId(Integer userId);

	@Query("SELECT o FROM Order o " +
	           "JOIN FETCH o.product p " +
	           "JOIN FETCH o.bid b " +
	           "WHERE o.user.id = :buyerId")
	    List<Order> findAllByBuyerIdWithDetails(int buyerId);

	    @Query("SELECT o FROM Order o " +
	           "JOIN FETCH o.product p " +
	           "JOIN FETCH o.bid b " +
	           "WHERE p.seller.id = :sellerId")
	    List<Order> findAllBySellerIdWithDetails(int sellerId);
	
}

