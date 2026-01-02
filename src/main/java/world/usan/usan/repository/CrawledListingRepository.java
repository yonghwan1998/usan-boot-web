package world.usan.usan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import world.usan.usan.entity.CrawledListing;

import java.util.UUID;

public interface CrawledListingRepository extends JpaRepository<CrawledListing, UUID> {
    boolean existsByCode(UUID code);
}
