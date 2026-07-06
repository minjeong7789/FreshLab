package com.freshlab.freshdoctor.dto;

public record NewsCollectResult(
        String itemCode,
        String query,
        int fetchedCount,
        int savedCount,
        String message
) {
}
