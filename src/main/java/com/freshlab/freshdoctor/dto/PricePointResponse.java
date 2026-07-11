package com.freshlab.freshdoctor.dto;

import java.time.LocalDate;

public record PricePointResponse(
        LocalDate date,
        Integer price,
        LocalDate actualPriceDate,
        boolean carriedForward
) {
}
