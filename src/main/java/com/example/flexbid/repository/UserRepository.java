package com.example.flexbid.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.flexbid.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
	List<User> findByEmailVerifiedFalseAndCreatedAtBefore(LocalDateTime cutoff);
	Optional<User> findById(User user);
	
}
