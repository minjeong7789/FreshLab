package com.freshlab.freshdoctor.dto;

import java.math.BigDecimal;

public record RecommendationInput(
        String itemCode,
        String itemName,
        String riskGrade,
        Integer finalScore,
        BigDecimal priceIncreaseRate,
        BigDecimal normalYearComparisonRate,
        BigDecimal priceVolatilityRate,
        String weatherIssue,
        String newsIssue
) {
}
