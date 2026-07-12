package com.freshlab.freshdoctor.dto;

import java.time.LocalDate;

public record WeatherRiskResponse(
        ComparisonStatus status,
        String itemCode,
        String region,
        WeatherRiskType riskType,
        Integer score,
        String reason,
        LocalDate baseDate,
        String baseTime,
        LocalDate forecastDate,
        String forecastTime,
        String source,
        String unavailableReason
) {
}
