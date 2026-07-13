package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.domain.PriceHistory;
import com.freshlab.freshdoctor.dto.DashboardResponse;
import com.freshlab.freshdoctor.dto.RiskScoreResponse;
import com.freshlab.freshdoctor.repository.ItemRepository;
import com.freshlab.freshdoctor.repository.PriceHistoryRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private final ItemRepository itemRepository = mock(ItemRepository.class);
    private final PriceHistoryRepository priceHistoryRepository = mock(PriceHistoryRepository.class);
    private final RiskScoreService riskScoreService = mock(RiskScoreService.class);
    private final RiskDashboardService riskDashboardService = mock(RiskDashboardService.class);
    private final DashboardService dashboardService = new DashboardService(
            itemRepository,
            priceHistoryRepository,
            riskScoreService,
            riskDashboardService
    );

    @Test
    void buildsDashboardSortedByScoreChangeRateAndItemName() {
        Item cabbage = item("1001", "배추");
        Item radish = item("1002", "무");
        Item greenOnion = item("1003", "대파");
        when(itemRepository.findByActiveTrueOrderByItemNameAsc())
                .thenReturn(List.of(radish, cabbage, greenOnion));

        when(riskScoreService.getLatest("1001"))
                .thenReturn(risk("1001", 82, "ALERT", new BigDecimal("12.5")));
        when(riskScoreService.getLatest("1002"))
                .thenReturn(risk("1002", 45, "WATCH", new BigDecimal("3.0")));
        when(riskScoreService.getLatest("1003"))
                .thenReturn(risk("1003", 82, "ALERT", new BigDecimal("18.0")));

        when(priceHistoryRepository.findTopByItemCodeAndMarketTypeAndKamisRankCodeAndUnitOrderByPriceDateDesc(
                "1001", "소매", "04", "1kg"
        )).thenReturn(Optional.of(price("1001", 6_000, "1kg")));
        when(priceHistoryRepository.findTopByItemCodeAndMarketTypeAndKamisRankCodeAndUnitOrderByPriceDateDesc(
                "1002", "소매", "04", "1개"
        )).thenReturn(Optional.of(price("1002", 2_000, "1개")));
        when(priceHistoryRepository.findTopByItemCodeAndMarketTypeAndKamisRankCodeAndUnitOrderByPriceDateDesc(
                "1003", "소매", "04", "1kg"
        )).thenReturn(Optional.of(price("1003", 4_000, "1kg")));

        DashboardResponse response = dashboardService.getDashboard();

        assertThat(response.todayScore()).isEqualTo(82);
        assertThat(response.todayGrade()).isEqualTo("ALERT");
        assertThat(response.summary()).isEqualTo("오늘은 대파·배추 발주를 조심하세요.");
        assertThat(response.topRiskItems()).extracting("itemName").containsExactly("대파", "배추");
        assertThat(response.gradeCounts().watch()).isEqualTo(1);
        assertThat(response.gradeCounts().alert()).isEqualTo(2);
        assertThat(response.items()).extracting("itemName").containsExactly("대파", "배추", "무");
        assertThat(response.aiRecommendation()).isEqualTo("발주량을 보수적으로 조정하고 가격 변동을 다시 확인하세요.");
        assertThat(response.dataDate()).isEqualTo(LocalDate.of(2026, 7, 24));
    }

    private Item item(String itemCode, String itemName) {
        Item item = new Item();
        item.setItemCode(itemCode);
        item.setItemName(itemName);
        item.setDefaultMarketType("소매");
        item.setDefaultRankCode("04");
        item.setDefaultUnit(itemCode.equals("1002") ? "1개" : "1kg");
        item.setItemType(Item.ItemType.DOMESTIC);
        item.setActive(true);
        return item;
    }

    private RiskScoreResponse risk(String itemCode, Integer finalScore, String grade, BigDecimal increaseRate) {
        return new RiskScoreResponse(
                1L,
                itemCode,
                LocalDate.of(2026, 7, 24),
                increaseRate,
                10,
                new BigDecimal("15.0"),
                10,
                new BigDecimal("5.0"),
                5,
                "NONE",
                0,
                null,
                "NONE",
                0,
                null,
                null,
                35,
                finalScore,
                grade,
                null,
                null,
                LocalDateTime.of(2026, 7, 24, 9, 0),
                LocalDateTime.of(2026, 7, 24, 10, 0)
        );
    }

    private PriceHistory price(String itemCode, Integer price, String unit) {
        PriceHistory history = new PriceHistory();
        history.setItemCode(itemCode);
        history.setItemName(itemCode);
        history.setPrice(price);
        history.setUnit(unit);
        history.setMarketType("소매");
        history.setKamisRankCode("04");
        history.setPriceDate(LocalDate.of(2026, 7, 24));
        history.setUpdatedAt(LocalDateTime.of(2026, 7, 24, 8, 30));
        return history;
    }
}
