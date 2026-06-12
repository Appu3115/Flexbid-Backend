package com.example.flexbid.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.flexbid.model.PasswordResetToken;


@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordResetToken, Integer> {
    Optional<PasswordResetToken> findByToken(String token);
    List<PasswordResetToken> findByUserId(int i);
	void deleteByUserId(Integer userId);
	
}
