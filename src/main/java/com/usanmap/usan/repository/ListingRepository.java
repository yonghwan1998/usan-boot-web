package com.usanmap.usan.repository;

import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.usanmap.usan.entity.enums.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import com.usanmap.usan.entity.Listing;

import java.util.List;
import java.util.Optional;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Listing l where l.id = :id")
    Optional<Listing> lockForSending(@Param("id") Long id);

    Optional<Listing> findByPublicId(String publicId);

    List<Listing> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    List<Listing> findAllByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, ListingStatus status);
}
