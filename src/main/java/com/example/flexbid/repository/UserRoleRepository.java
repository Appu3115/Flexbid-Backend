package com.example.flexbid.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.flexbid.model.Role;
import com.example.flexbid.model.User;
import com.example.flexbid.model.UserRole;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {
//    List<UserRole> findByUser(User user);
    boolean existsByUserAndRole(User user, Role role);
    boolean existsByRole(Role role);
    void deleteByUser(User user);
    boolean existsByUserIdAndRole(Integer userId, Role role);
	List<UserRole> findByUserId(Integer id);
	void deleteByUserId(Integer userId);
}

