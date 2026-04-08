package com.usanmap.usan.repository;

import com.usanmap.usan.entity.CreditProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditProductRepository extends JpaRepository<CreditProduct, Long> {

    List<CreditProduct> findByIsActiveTrueOrderBySortOrderAsc();

    Optional<CreditProduct> findByProductCode(String productCode);
}
