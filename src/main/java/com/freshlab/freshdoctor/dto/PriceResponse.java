package com.freshlab.freshdoctor.dto;

import com.freshlab.freshdoctor.domain.PriceHistory;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PriceResponse(
        Long id,
        String itemCode,
        String itemName,
        String kamisItemCode,
        String kamisKindCode,
        String kamisRankCode,
        LocalDate priceDate,
        Integer price,
        Integer normalYearPrice,
        String unit,
        String marketType,
        LocalDateTime createdAt
) {

    public static PriceResponse from(PriceHistory priceHistory) {
        return new PriceResponse(
                priceHistory.getId(),
                priceHistory.getItemCode(),
                priceHistory.getItemName(),
                priceHistory.getKamisItemCode(),
                priceHistory.getKamisKindCode(),
                priceHistory.getKamisRankCode(),
                priceHistory.getPriceDate(),
                priceHistory.getPrice(),
                priceHistory.getNormalYearPrice(),
                priceHistory.getUnit(),
                priceHistory.getMarketType(),
                priceHistory.getCreatedAt()
        );
    }
}
