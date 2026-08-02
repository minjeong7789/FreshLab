package com.freshlab.freshdoctor.dto;

import com.freshlab.freshdoctor.domain.RiskScore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RiskScoreResponse(
        Long id,
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
        String newsRiskType,
        Integer newsScore,
        String newsReason,
        Long representativeNewsArticleId,
        Integer rawScore,
        Integer finalScore,
        String riskGrade,
        String unavailableItems,
        String unavailableReasons,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static RiskScoreResponse from(RiskScore riskScore) {
        return new RiskScoreResponse(
                riskScore.getId(),
                riskScore.getItemCode(),
                riskScore.getScoreDate(),
                riskScore.getPriceIncreaseRate(),
                riskScore.getPriceIncreaseScore(),
                riskScore.getNormalYearComparisonRate(),
                riskScore.getNormalYearScore(),
                riskScore.getPriceVolatilityRate(),
                riskScore.getVolatilityScore(),
                riskScore.getWeatherRiskType(),
                riskScore.getWeatherScore(),
                riskScore.getWeatherReason(),
                riskScore.getNewsRiskType(),
                riskScore.getNewsScore(),
                riskScore.getNewsReason(),
                riskScore.getRepresentativeNewsArticleId(),
                riskScore.getRawScore(),
                riskScore.getFinalScore(),
                riskScore.getRiskGrade(),
                riskScore.getUnavailableItems(),
                riskScore.getUnavailableReasons(),
                riskScore.getCreatedAt(),
                riskScore.getUpdatedAt()
        );
    }
}
