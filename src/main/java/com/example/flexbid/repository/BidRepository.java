package com.example.flexbid.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.flexbid.model.Bid;
//import com.example.flexbid.model.Product;

@Repository
public interface BidRepository extends JpaRepository<Bid, Integer> {

    // Custom query to find all bids for a specific product
    List<Bid> findByProductId(int productId);

    // Custom query to find all bids for a specific buyer
    List<Bid> findByBuyerId(int buyerId);

    // Custom query to get the latest bid for a product (sorted by creation time)
//    Bid findTopByProductIdOrderByCreatedAtDesc(int productId);
    Bid findTopByProductIdOrderByBidTimeDesc(int productId);

	boolean existsByProductId(int productId);

	List<Bid> findByProductIdOrderByBidTimeDesc(int productId);
	
	@Query("SELECT SUM(b.quantity) FROM Bid b WHERE b.productId = :productId")
	Integer sumQuantityByProductId(@Param("productId") int productId);

	Bid findTopByProductIdAndBiddingRoundOrderByBidTimeDesc(int id, int currentBiddingRound);
	
	@Query("SELECT DISTINCT b.biddingRound FROM Bid b WHERE b.productId = :productId")
	List<Integer> findDistinctRoundsByProductId(@Param("productId") int productId);

	// Find the highest bid manually by amount then earliest time
	@Query("SELECT b FROM Bid b WHERE b.productId = :productId AND b.biddingRound = :round")
	List<Bid> findAllByProductIdAndRound(@Param("productId") int productId, @Param("round") int round);

	@Query("SELECT b FROM Bid b WHERE b.productId = :productId AND b.biddingRound = :biddingRound ORDER BY b.amount DESC, b.bidTime ASC")
	List<Bid> findAllByProductIdAndBiddingRoundOrderByAmountDescTimeAsc(@Param("productId") int productId, @Param("biddingRound") int biddingRound);

	void deleteByBuyerId(int buyerId);

	List<Bid> findByProductIdAndBiddingRound(int productId, int biddingRound);


//	Optional<Bid> findTopByProductIdOrderByBiddingRoundDescBidTimeDesc(int productId);

	List<Bid> findByProductIdAndBiddingRoundOrderByBidTimeDesc(int productId, int biddingRound);

	Optional<Bid> findTopByProductIdAndLeftBidFalseOrderByBiddingRoundDescBidTimeDesc(int productId);

	List<Bid> findByProductIdAndBiddingRoundAndLeftBidFalseOrderByBidTimeDesc(int productId, int currentRound);

	Optional<Bid> findTopByProductIdAndBiddingRoundAndLeftBidFalseOrderByBidTimeDesc(int productId,
			int currentBiddingRound);

	List<Bid> findByBuyerIdAndWinningBidTrue(int buyerId);


	

}
