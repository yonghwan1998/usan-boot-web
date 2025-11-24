package world.usan.usan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import world.usan.usan.entity.Broker;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrokerRepository extends JpaRepository<Broker, UUID> {
    Optional<Broker> findByRegistrationNumberAndBrokerName(String registrationNumber, String brokerName);
    Broker findByBrokerCode(UUID brokerCode);
    List<Broker> findAllByRegistrationNumber(String registrationNumber);
}
