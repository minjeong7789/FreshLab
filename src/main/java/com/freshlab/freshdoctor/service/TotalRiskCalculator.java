package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.dto.RiskGrade;
import com.freshlab.freshdoctor.dto.TotalRiskCalculationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Component
public class TotalRiskCalculator {

    private static final int RAW_SCORE_MAX = 85;
    private static final int FINAL_SCORE_MAX = 100;

    public TotalRiskCalculationResult calculate(
            String itemCode,
            LocalDate scoreDate,
            Integer priceIncreaseScore,
            Integer normalYearScore,
            Integer volatilityScore,
            Integer weatherScore,
            Integer newsScore,
            LocalDate latestValidDataDate,
            List<String> unavailableItems,
            List<String> unavailableReasons
    ) {
        int rawScore = sumScores(
                priceIncreaseScore,
                normalYearScore,
                volatilityScore,
                weatherScore,
                newsScore
        );
        int finalScore = normalize(rawScore);
        return new TotalRiskCalculationResult(
                itemCode,
                scoreDate,
                rawScore,
                finalScore,
                resolveRiskGrade(finalScore),
                latestValidDataDate,
                null,
                unavailableItems == null ? List.of() : List.copyOf(unavailableItems),
                unavailableReasons == null ? List.of() : List.copyOf(unavailableReasons)
        );
    }

    public int normalize(int rawScore) {
        if (rawScore <= 0) {
            return 0;
        }
        BigDecimal normalized = BigDecimal.valueOf(rawScore)
                .multiply(BigDecimal.valueOf(FINAL_SCORE_MAX))
                .divide(BigDecimal.valueOf(RAW_SCORE_MAX), 0, RoundingMode.HALF_UP);
        return Math.min(normalized.intValue(), FINAL_SCORE_MAX);
    }

    public RiskGrade resolveRiskGrade(int finalScore) {
        if (finalScore >= 85) {
            return RiskGrade.SEVERE;
        }
        if (finalScore >= 70) {
            return RiskGrade.ALERT;
        }
        if (finalScore >= 50) {
            return RiskGrade.CAUTION;
        }
        if (finalScore >= 30) {
            return RiskGrade.WATCH;
        }
        return RiskGrade.STABLE;
    }

    private int sumScores(Integer... scores) {
        int total = 0;
        for (Integer score : scores) {
            if (score != null) {
                total += score;
            }
        }
        return total;
    }
}
