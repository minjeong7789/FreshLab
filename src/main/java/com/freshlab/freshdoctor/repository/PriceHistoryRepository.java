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

    List<PriceHistory> findByItemCodeAndMarketTypeAndKamisRankCodeAndUnitAndPriceDateBetweenOrderByPriceDateAsc(
            String itemCode,
            String marketType,
            String kamisRankCode,
            String unit,
            LocalDate startDate,
            LocalDate endDate
    );

    List<PriceHistory> findTop60ByItemCodeOrderByPriceDateDesc(String itemCode);

    List<PriceHistory> findByItemCodeAndPriceDateOrderByIdAsc(String itemCode, LocalDate priceDate);

    List<PriceHistory> findTop60ByItemCodeAndMarketTypeAndKamisRankCodeAndUnitOrderByPriceDateDesc(
            String itemCode,
            String marketType,
            String kamisRankCode,
            String unit
    );

    List<PriceHistory> findTop7ByItemCodeAndMarketTypeAndKamisRankCodeAndUnitOrderByPriceDateDesc(
            String itemCode,
            String marketType,
            String kamisRankCode,
            String unit
    );

    Optional<PriceHistory> findTopByItemCodeAndMarketTypeAndKamisRankCodeAndUnitAndPriceDateLessThanEqualOrderByPriceDateDesc(
            String itemCode,
            String marketType,
            String kamisRankCode,
            String unit,
            LocalDate priceDate
    );

    List<PriceHistory> findByItemNameContainingAndPriceDateBetweenOrderByPriceDateAsc(
            String itemName,
            LocalDate startDate,
            LocalDate endDate
    );

    List<PriceHistory> findTop60ByItemNameContainingOrderByPriceDateDesc(String itemName);

    Optional<PriceHistory> findTopByItemCodeOrderByPriceDateDesc(String itemCode);

    Optional<PriceHistory> findTopByItemCodeAndMarketTypeAndKamisRankCodeAndUnitOrderByPriceDateDesc(
            String itemCode,
            String marketType,
            String kamisRankCode,
            String unit
    );

}
