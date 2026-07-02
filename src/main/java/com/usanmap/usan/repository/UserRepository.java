package com.usanmap.usan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.usanmap.usan.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmailAndPhone(String email, String phone);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
}
