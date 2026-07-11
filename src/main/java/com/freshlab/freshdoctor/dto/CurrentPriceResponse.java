package com.freshlab.freshdoctor.dto;

import java.time.LocalDate;

public record CurrentPriceResponse(
        Integer price,
        String unit,
        LocalDate baseDate,
        LocalDate actualPriceDate,
        boolean carriedForward
) {
}
