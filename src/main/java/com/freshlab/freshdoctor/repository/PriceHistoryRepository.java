package com.freshlab.freshdoctor.repository;

import com.freshlab.freshdoctor.domain.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByItemCodeAndPriceDateBetweenOrderByPriceDateAsc(
            String itemCode,
            LocalDate startDate,
            LocalDate endDate
    );

    List<PriceHistory> findTop60ByItemCodeOrderByPriceDateDesc(String itemCode);

    List<PriceHistory> findByItemNameContainingAndPriceDateBetweenOrderByPriceDateAsc(
            String itemName,
            LocalDate startDate,
            LocalDate endDate
    );

    List<PriceHistory> findTop60ByItemNameContainingOrderByPriceDateDesc(String itemName);

    Optional<PriceHistory> findTopByItemCodeOrderByPriceDateDesc(String itemCode);

    Optional<PriceHistory> findByItemCodeAndPriceDateAndMarketTypeAndSource(
            String itemCode,
            LocalDate priceDate,
            String marketType,
            String source
    );

    Optional<PriceHistory> findByItemNameAndPriceDateAndUnitAndSource(
            String itemName,
            LocalDate priceDate,
            String unit,
            String source
    );
}
