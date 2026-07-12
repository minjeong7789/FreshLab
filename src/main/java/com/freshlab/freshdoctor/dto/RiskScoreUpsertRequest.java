package com.freshlab.freshdoctor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RiskScoreUpsertRequest(
        String itemCode,
        LocalDate scoreDate,
        BigDecimal priceIncreaseRate,
        Integer priceIncreaseScore,
        BigDecimal normalYearComparisonRate,
        Integer normalYearScore,
        BigDecimal priceVolatilityRate,
        Integer volatilityScore,
        String weatherRiskType,
        Integer weatherScore,
        String weatherReason,
        LocalDate weatherBaseDate,
        String weatherBaseTime,
        String newsRiskType,
        Integer newsScore,
        String newsReason,
        Long representativeNewsArticleId,
        String unavailableItems,
        String unavailableReasons
) {
}
