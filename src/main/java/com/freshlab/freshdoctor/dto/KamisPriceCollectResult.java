package com.freshlab.freshdoctor.dto;

public record KamisPriceCollectResult(
        String itemCode,
        int fetchedCount,
        int savedCount,
        String message
) {
}
