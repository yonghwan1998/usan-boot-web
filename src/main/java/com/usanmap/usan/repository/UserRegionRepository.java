package com.usanmap.usan.repository;

import com.usanmap.usan.entity.UserRegion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRegionRepository extends JpaRepository<UserRegion, Long> {

    List<UserRegion> findByUserIdOrderByCreatedAtDesc(Long userId);
}
