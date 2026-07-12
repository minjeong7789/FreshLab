package com.freshlab.freshdoctor.dto;

import java.time.LocalDate;
import java.util.List;

public record TotalRiskCalculationResult(
        String itemCode,
        LocalDate scoreDate,
        Integer rawScore,
        Integer finalScore,
        RiskGrade riskGrade,
        LocalDate latestValidDataDate,
        RiskScoreResponse savedRiskScore,
        List<String> unavailableItems,
        List<String> unavailableReasons
) {
}
