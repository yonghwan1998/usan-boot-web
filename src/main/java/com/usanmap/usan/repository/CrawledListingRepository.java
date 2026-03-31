package com.usanmap.usan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.usanmap.usan.entity.CrawledListing;

import java.util.UUID;

public interface CrawledListingRepository extends JpaRepository<CrawledListing, UUID> {
    boolean existsByCode(UUID code);
}
