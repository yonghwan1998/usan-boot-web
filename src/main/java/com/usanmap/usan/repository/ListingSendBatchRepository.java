package com.usanmap.usan.repository;

import com.usanmap.usan.entity.ListingSendBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface ListingSendBatchRepository extends JpaRepository<ListingSendBatch, Long> {
    Page<ListingSendBatch> findAllByUserIdOrderByRequestedAtDescIdDesc(Long userId, Pageable pageable);
    Optional<ListingSendBatch> findByIdAndUserId(Long id, Long userId);
}
