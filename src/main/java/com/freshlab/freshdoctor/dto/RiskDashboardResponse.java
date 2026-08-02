package com.freshlab.freshdoctor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RiskDashboardResponse(
        String itemCode,
        LocalDate scoreDate,
        Integer finalScore,
        String riskGrade,
        List<RiskFactorResponse> factors,
        BigDecimal priceIncreaseRate,
        BigDecimal normalYearComparisonRate,
        BigDecimal priceVolatilityRate,
        String weatherIssue,
        String newsIssue,
        LocalDate baseDate,
        LocalDateTime lastUpdatedAt,
        List<String> unavailableItems,
        List<String> unavailableReasons
) {
}
