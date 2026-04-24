package com.usanmap.usan.repository;

import com.usanmap.usan.entity.ListingSendHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingSendHistoryRepository extends JpaRepository<ListingSendHistory, Long> {

    List<ListingSendHistory> findAllByListingIdOrderBySentAtDesc(Long listingId);
}
