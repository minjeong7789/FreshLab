package com.freshlab.freshdoctor.dto;

import java.math.BigDecimal;

public record PriceVolatilityResponse(
        ComparisonStatus status,
        BigDecimal volatilityRate,
        Integer score,
        Integer usedPriceCount,
        String unavailableReason
) {
}
