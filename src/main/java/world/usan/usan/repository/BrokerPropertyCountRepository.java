package world.usan.usan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import world.usan.usan.entity.BrokerPropertyCount;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BrokerPropertyCountRepository extends JpaRepository<BrokerPropertyCount, UUID> {

    List<BrokerPropertyCount> findByLatBetweenAndLngBetween(
            BigDecimal south, BigDecimal north,
            BigDecimal west, BigDecimal east);
    BrokerPropertyCount findByBrokerCode(UUID brokerCode);
    List<BrokerPropertyCount> findByBrokerCodeIn(Collection<UUID> brokerCodes);
}
