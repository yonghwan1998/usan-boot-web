package com.usanmap.usan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.usanmap.usan.entity.BrokerPropertyCount;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrokerPropertyCountRepository extends JpaRepository<BrokerPropertyCount, UUID> {

    List<BrokerPropertyCount> findByLatBetweenAndLngBetween(
            BigDecimal south, BigDecimal north,
            BigDecimal west, BigDecimal east);
    List<BrokerPropertyCount> findByBrokerCodeIn(Collection<UUID> brokerCodes);

    List<BrokerPropertyCount> findBySidoAndSigunguAndEmd(String sido, String sigungu, String emd);

    Optional<BrokerPropertyCount> findByPublicId(String publicId);
}
