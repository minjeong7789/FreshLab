package com.freshlab.freshdoctor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DashboardItemResponse(
        String itemCode,
        String itemName,
        Integer currentPrice,
        String unit,
        BigDecimal sevenDayChangeRate,
        Integer finalScore,
        String riskGrade,
        LocalDate dataDate,
        LocalDateTime lastUpdatedAt
) {
}
