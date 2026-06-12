package com.example.flexbid.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.flexbid.model.EmailVerificationToken;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Integer> {
    Optional<EmailVerificationToken> findByToken(String token);

	void deleteByExpiryTimeBefore(LocalDateTime now);

	List<EmailVerificationToken> findByExpiryTimeBeforeAndUsedFalse(LocalDateTime now);

	void deleteByUserId(Integer id);

	Iterable<? extends EmailVerificationToken> findByUserId(Integer id);

//	EmailVerificationToken findTopByUserIdAndTokenAndUsedFalseOrderByExpiryTimeDesc(Integer id, String otp);
	
}

