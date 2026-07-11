package com.freshlab.freshdoctor.repository;

import com.freshlab.freshdoctor.domain.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Modifying
    @Query(value = """
            INSERT INTO price_history (
                item_code,
                item_name,
                kamis_item_code,
                kamis_kind_code,
                kamis_rank_code,
                price_date,
                price,
                normal_year_price,
                unit,
                market_type,
                created_at,
                updated_at
            ) VALUES (
                :itemCode,
                :itemName,
                :kamisItemCode,
                :kamisKindCode,
                :kamisRankCode,
                :priceDate,
                :price,
                :normalYearPrice,
                :unit,
                :marketType,
                :now,
                :now
            )
            ON DUPLICATE KEY UPDATE
                item_name = VALUES(item_name),
                kamis_item_code = VALUES(kamis_item_code),
                kamis_kind_code = VALUES(kamis_kind_code),
                price = VALUES(price),
                normal_year_price = VALUES(normal_year_price),
                updated_at = VALUES(updated_at)
            """, nativeQuery = true)
    int upsert(
            @Param("itemCode") String itemCode,
            @Param("itemName") String itemName,
            @Param("kamisItemCode") String kamisItemCode,
            @Param("kamisKindCode") String kamisKindCode,
            @Param("kamisRankCode") String kamisRankCode,
            @Param("priceDate") LocalDate priceDate,
            @Param("price") Integer price,
            @Param("normalYearPrice") Integer normalYearPrice,
            @Param("unit") String unit,
            @Param("marketType") String marketType,
            @Param("now") LocalDateTime now
    );
}
