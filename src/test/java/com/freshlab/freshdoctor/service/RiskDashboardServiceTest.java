package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.dto.RiskDashboardResponse;
import com.freshlab.freshdoctor.dto.RiskScoreResponse;
import com.freshlab.freshdoctor.repository.ItemRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RiskDashboardServiceTest {

    private final RiskDashboardService service = new RiskDashboardService(
            mock(RiskScoreService.class),
            mock(TotalRiskService.class),
            mock(ItemRepository.class)
    );

    @Test
    void convertsRiskScoreToDashboardResponseWithFactorRatios() {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 7, 23, 10, 30);
        RiskScoreResponse riskScore = new RiskScoreResponse(
                1L,
                "1001",
                LocalDate.of(2026, 7, 23),
                new BigDecimal("12.50"),
                10,
                new BigDecimal("18.20"),
                15,
                new BigDecimal("8.40"),
                10,
                "HEAT_WAVE",
                10,
                "예상 기온이 33도 이상입니다.",
                "SHIPMENT_DECREASE",
                5,
                "출하량 감소 또는 수급 불안 신호가 감지되었습니다.",
                12L,
                50,
                59,
                "CAUTION",
                "normalYear,volatility",
                "평년 가격 없음; 최근 7개 가격 부족",
                LocalDateTime.of(2026, 7, 23, 10, 0),
                updatedAt
        );

        RiskDashboardResponse response = service.toDashboardResponse(riskScore);

        assertThat(response.itemCode()).isEqualTo("1001");
        assertThat(response.finalScore()).isEqualTo(59);
        assertThat(response.riskGrade()).isEqualTo("CAUTION");
        assertThat(response.factors()).hasSize(5);
        assertThat(response.factors().get(0).name()).isEqualTo("PRICE_INCREASE");
        assertThat(response.factors().get(0).displayRatio()).isEqualTo(50);
        assertThat(response.factors().get(2).name()).isEqualTo("VOLATILITY");
        assertThat(response.factors().get(2).displayRatio()).isEqualTo(67);
        assertThat(response.weatherIssue()).isEqualTo("예상 기온이 33도 이상입니다.");
        assertThat(response.newsIssue()).isEqualTo("출하량 감소 또는 수급 불안 신호가 감지되었습니다.");
        assertThat(response.baseDate()).isEqualTo(LocalDate.of(2026, 7, 23));
        assertThat(response.lastUpdatedAt()).isEqualTo(updatedAt);
        assertThat(response.unavailableItems()).containsExactly("normalYear", "volatility");
        assertThat(response.unavailableReasons()).containsExactly("평년 가격 없음", "최근 7개 가격 부족");
    }
}
