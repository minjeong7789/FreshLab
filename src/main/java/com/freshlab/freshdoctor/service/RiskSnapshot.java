package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.RiskScore;

import java.math.BigDecimal;

public record RiskSnapshot(
        Integer score,
        String grade,
        BigDecimal priceIncreaseRate,
        BigDecimal priceVolatilityRate,
        String weatherRiskType,
        Integer weatherScore,
        String weatherReason,
        String newsRiskType,
        Integer newsScore,
        String newsReason
) {
    public static RiskSnapshot from(RiskScore riskScore) {
        if (riskScore == null) {
            return null;
        }
        return new RiskSnapshot(
                riskScore.getFinalScore(),
                riskScore.getRiskGrade(),
                riskScore.getPriceIncreaseRate(),
                riskScore.getPriceVolatilityRate(),
                riskScore.getWeatherRiskType(),
                riskScore.getWeatherScore(),
                riskScore.getWeatherReason(),
                riskScore.getNewsRiskType(),
                riskScore.getNewsScore(),
                riskScore.getNewsReason()
        );
    }
}
