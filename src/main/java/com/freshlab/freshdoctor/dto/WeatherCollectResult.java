package com.freshlab.freshdoctor.dto;

public record WeatherCollectResult(
        String itemCode,
        String region,
        int fetchedCount,
        int savedCount,
        String message
) {
}
