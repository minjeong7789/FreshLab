package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.domain.PriceHistory;
import com.freshlab.freshdoctor.dto.DashboardGradeCountsResponse;
import com.freshlab.freshdoctor.dto.DashboardItemResponse;
import com.freshlab.freshdoctor.dto.DashboardResponse;
import com.freshlab.freshdoctor.dto.DashboardTopRiskItemResponse;
import com.freshlab.freshdoctor.dto.RiskDashboardResponse;
import com.freshlab.freshdoctor.dto.RiskGrade;
import com.freshlab.freshdoctor.dto.RiskScoreResponse;
import com.freshlab.freshdoctor.repository.ItemRepository;
import com.freshlab.freshdoctor.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ItemRepository itemRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final RiskScoreService riskScoreService;
    private final RiskDashboardService riskDashboardService;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        List<DashboardItem> dashboardItems = itemRepository.findByActiveTrueOrderByItemNameAsc()
                .stream()
                .map(this::toDashboardItem)
                .sorted(DashboardItem.ORDERING)
                .toList();

        Integer todayScore = dashboardItems.stream()
                .map(DashboardItem::finalScore)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        String todayGrade = resolveRiskGrade(todayScore).name();

        List<DashboardTopRiskItemResponse> topRiskItems = dashboardItems.stream()
                .filter(item -> item.finalScore() != null && item.finalScore() >= 41)
                .limit(2)
                .map(item -> new DashboardTopRiskItemResponse(
                        item.itemCode(),
                        item.itemName(),
                        item.finalScore(),
                        item.riskGrade()
                ))
                .toList();

        return new DashboardResponse(
                todayScore,
                todayGrade,
                buildSummary(topRiskItems),
                topRiskItems,
                countGrades(dashboardItems),
                dashboardItems.stream().map(DashboardItem::response).toList(),
                buildRecommendation(todayGrade),
                latestDataDate(dashboardItems),
                latestUpdatedAt(dashboardItems)
        );
    }

    @Transactional(readOnly = true)
    public RiskDashboardResponse getItemDashboard(String itemCode) {
        return riskDashboardService.getLatestRisk(itemCode);
    }

    private DashboardItem toDashboardItem(Item item) {
        RiskScoreResponse riskScore = riskScoreService.getLatest(item.getItemCode());
        PriceHistory latestPrice = priceHistoryRepository
                .findTopByItemCodeAndMarketTypeAndKamisRankCodeAndUnitOrderByPriceDateDesc(
                        item.getItemCode(),
                        item.getDefaultMarketType(),
                        item.getDefaultRankCode(),
                        item.getDefaultUnit()
                )
                .orElse(null);

        DashboardItemResponse response = new DashboardItemResponse(
                item.getItemCode(),
                item.getItemName(),
                latestPrice == null ? null : latestPrice.getPrice(),
                latestPrice == null ? item.getDefaultUnit() : latestPrice.getUnit(),
                riskScore == null ? null : riskScore.priceIncreaseRate(),
                riskScore == null ? null : riskScore.finalScore(),
                riskScore == null ? RiskGrade.SAFE.name() : riskScore.riskGrade(),
                resolveDataDate(riskScore, latestPrice),
                resolveUpdatedAt(riskScore, latestPrice)
        );

        return new DashboardItem(
                response.itemCode(),
                response.itemName(),
                response.finalScore(),
                response.sevenDayChangeRate(),
                response.riskGrade(),
                response.dataDate(),
                response.lastUpdatedAt(),
                response
        );
    }

    private LocalDate resolveDataDate(RiskScoreResponse riskScore, PriceHistory latestPrice) {
        if (riskScore != null && riskScore.scoreDate() != null) {
            return riskScore.scoreDate();
        }
        return latestPrice == null ? null : latestPrice.getPriceDate();
    }

    private LocalDateTime resolveUpdatedAt(RiskScoreResponse riskScore, PriceHistory latestPrice) {
        if (riskScore != null && riskScore.updatedAt() != null) {
            return riskScore.updatedAt();
        }
        return latestPrice == null ? null : latestPrice.getUpdatedAt();
    }

    private DashboardGradeCountsResponse countGrades(List<DashboardItem> items) {
        return new DashboardGradeCountsResponse(
                countGrade(items, RiskGrade.SAFE),
                countGrade(items, RiskGrade.INTEREST),
                countGrade(items, RiskGrade.CAUTION),
                countGrade(items, RiskGrade.ALERT),
                countGrade(items, RiskGrade.CRITICAL)
        );
    }

    private int countGrade(List<DashboardItem> items, RiskGrade grade) {
        return (int) items.stream()
                .filter(item -> grade.name().equals(item.riskGrade()))
                .count();
    }

    private String buildSummary(List<DashboardTopRiskItemResponse> topRiskItems) {
        if (topRiskItems.isEmpty()) {
            return "오늘은 전반적으로 발주 위험이 낮습니다.";
        }
        String itemNames = topRiskItems.stream()
                .map(DashboardTopRiskItemResponse::itemName)
                .filter(Objects::nonNull)
                .reduce((left, right) -> left + "·" + right)
                .orElse("주요 품목");
        return "오늘은 " + itemNames + " 발주를 조심하세요.";
    }

    private String buildRecommendation(String todayGrade) {
        RiskGrade grade = RiskGrade.valueOf(todayGrade);
        return switch (grade) {
            case CRITICAL -> "즉시 재고를 점검하고 선발주 또는 대체 품목을 검토하세요.";
            case ALERT -> "발주량을 보수적으로 조정하고 가격 변동을 다시 확인하세요.";
            case CAUTION -> "일괄 발주보다 분할 발주를 고려하세요.";
            case INTEREST -> "가격과 수급 추이를 관찰하세요.";
            case SAFE -> "평소 발주 계획을 유지해도 무리가 적습니다.";
        };
    }

    private LocalDate latestDataDate(List<DashboardItem> items) {
        return items.stream()
                .map(DashboardItem::dataDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    private LocalDateTime latestUpdatedAt(List<DashboardItem> items) {
        return items.stream()
                .map(DashboardItem::lastUpdatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private RiskGrade resolveRiskGrade(int finalScore) {
        return RiskGrade.fromScore(finalScore);
    }

    private record DashboardItem(
            String itemCode,
            String itemName,
            Integer finalScore,
            BigDecimal sevenDayChangeRate,
            String riskGrade,
            LocalDate dataDate,
            LocalDateTime lastUpdatedAt,
            DashboardItemResponse response
    ) {
        private static final Comparator<DashboardItem> ORDERING = Comparator
                .comparing(DashboardItem::finalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(DashboardItem::sevenDayChangeRate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(DashboardItem::itemName, Comparator.nullsLast(Comparator.naturalOrder()));
    }
}
