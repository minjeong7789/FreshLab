package com.freshlab.freshdoctor.dto;

import com.freshlab.freshdoctor.domain.PriceHistory;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PriceResponse(
        Long id,
        String itemCode,
        String itemName,
        LocalDate priceDate,
        Integer price,
        String unit,
        String marketType,
        String source,
        LocalDateTime createdAt
) {

    public static PriceResponse from(PriceHistory priceHistory) {
        return new PriceResponse(
                priceHistory.getId(),
                priceHistory.getItemCode(),
                priceHistory.getItemName(),
                priceHistory.getPriceDate(),
                priceHistory.getPrice(),
                priceHistory.getUnit(),
                priceHistory.getMarketType(),
                priceHistory.getSource(),
                priceHistory.getCreatedAt()
        );
    }
}
