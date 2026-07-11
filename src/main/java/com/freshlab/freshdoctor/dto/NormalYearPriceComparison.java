package com.freshlab.freshdoctor.dto;

import java.math.BigDecimal;

public record NormalYearPriceComparison(
        ComparisonStatus status,
        Integer currentPrice,
        Integer normalYearPrice,
        BigDecimal comparisonRate,
        PriceDirection direction,
        Integer score
) {
}
