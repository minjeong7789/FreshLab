package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.dto.RiskGrade;
import com.freshlab.freshdoctor.dto.TotalRiskCalculationResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TotalRiskCalculatorTest {

    private final TotalRiskCalculator calculator = new TotalRiskCalculator();

    @Test
    void normalizesEightyFiveRawScoreToOneHundred() {
        TotalRiskCalculationResult result = calculator.calculate(
                "1001",
                LocalDate.of(2026, 7, 21),
                20,
                20,
                15,
                20,
                10,
                LocalDate.of(2026, 7, 21),
                List.of(),
                List.of()
        );

        assertThat(result.rawScore()).isEqualTo(85);
        assertThat(result.finalScore()).isEqualTo(100);
        assertThat(result.riskGrade()).isEqualTo(RiskGrade.SEVERE);
    }

    @Test
    void normalizesHalfRawScoreToFifty() {
        assertThat(calculator.normalize(43)).isEqualTo(51);
    }

    @Test
    void capsFinalScoreAtOneHundred() {
        assertThat(calculator.normalize(100)).isEqualTo(100);
    }

    @Test
    void appliesFiveRiskGrades() {
        assertThat(calculator.resolveRiskGrade(29)).isEqualTo(RiskGrade.STABLE);
        assertThat(calculator.resolveRiskGrade(30)).isEqualTo(RiskGrade.WATCH);
        assertThat(calculator.resolveRiskGrade(50)).isEqualTo(RiskGrade.CAUTION);
        assertThat(calculator.resolveRiskGrade(70)).isEqualTo(RiskGrade.ALERT);
        assertThat(calculator.resolveRiskGrade(85)).isEqualTo(RiskGrade.SEVERE);
    }

    @Test
    void keepsUnavailableItemsAndReasons() {
        TotalRiskCalculationResult result = calculator.calculate(
                "1001",
                LocalDate.of(2026, 7, 21),
                10,
                null,
                null,
                20,
                10,
                LocalDate.of(2026, 7, 21),
                List.of("normalYear", "volatility"),
                List.of("평년 가격 없음", "최근 7개 가격 부족")
        );

        assertThat(result.unavailableItems()).containsExactly("normalYear", "volatility");
        assertThat(result.unavailableReasons()).containsExactly("평년 가격 없음", "최근 7개 가격 부족");
    }
}
