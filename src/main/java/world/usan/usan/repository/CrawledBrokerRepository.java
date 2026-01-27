package world.usan.usan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import world.usan.usan.entity.CrawledBroker;

import java.util.Optional;
import java.util.UUID;

public interface CrawledBrokerRepository extends JpaRepository<CrawledBroker, UUID> {
    Optional<CrawledBroker> findByRegistrationNumberAndBrokerName(String registrationNumber, String brokerName);
}
