package com.freshlab.freshdoctor.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PriceTrendResponse(
        String itemCode,
        String itemName,
        CurrentPriceResponse current,
        Integer normalPrice,
        PriceChangeResponse priceChange,
        LocalDateTime lastUpdatedAt,
        List<PricePointResponse> prices
) {
}
