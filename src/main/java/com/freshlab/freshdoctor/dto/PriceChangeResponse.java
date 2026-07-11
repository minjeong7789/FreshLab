package com.freshlab.freshdoctor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceChangeResponse(
        PriceDirection direction,
        BigDecimal increaseRate,
        Integer score,
        Integer previousPrice,
        Integer latestPrice,
        LocalDate previousPriceDate,
        LocalDate latestPriceDate
) {
}
