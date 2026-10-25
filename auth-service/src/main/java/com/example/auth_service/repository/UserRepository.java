package com.example.auth_service.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.auth_service.entity.User;
import com.example.auth_service.entity.enums.Role;

public interface UserRepository extends JpaRepository<User,Long>{
    Optional<User> findByEmail(String email);
    Page<User> findByRole(Role role, Pageable pageable);
}
