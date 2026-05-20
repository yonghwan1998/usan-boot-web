package com.usanmap.usan.repository;

import com.usanmap.usan.entity.enums.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import com.usanmap.usan.entity.Listing;

import java.util.List;
import java.util.Optional;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    Optional<Listing> findByPublicId(String publicId);

    List<Listing> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    List<Listing> findAllByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, ListingStatus status);
}
