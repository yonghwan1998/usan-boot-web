package world.usan.usan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import world.usan.usan.entity.Listing;

import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID> {
    boolean existsByCode(UUID code);
}
