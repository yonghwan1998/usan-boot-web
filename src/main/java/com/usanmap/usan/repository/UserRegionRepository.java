package com.usanmap.usan.repository;

import com.usanmap.usan.entity.UserRegion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRegionRepository extends JpaRepository<UserRegion, Long> {

    Optional<UserRegion> findByUserId(Long userId);

    List<UserRegion> findByUserIdOrderByCreatedAtDesc(Long userId);
}
