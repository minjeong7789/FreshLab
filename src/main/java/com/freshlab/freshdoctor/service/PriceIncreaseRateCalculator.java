package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.dto.PriceChangeResponse;
import com.freshlab.freshdoctor.dto.PriceDirection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

@Component
public class PriceIncreaseRateCalculator {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal FIVE_PERCENT = BigDecimal.valueOf(5);
    private static final BigDecimal TEN_PERCENT = BigDecimal.valueOf(10);
    private static final BigDecimal TWENTY_PERCENT = BigDecimal.valueOf(20);

    public PriceChangeResponse calculate(
            int previousPrice,
            LocalDate previousPriceDate,
            int latestPrice,
            LocalDate latestPriceDate
    ) {
        validate(previousPrice, previousPriceDate, latestPrice, latestPriceDate);

        BigDecimal rawIncreaseRate = BigDecimal.valueOf(latestPrice)
                .subtract(BigDecimal.valueOf(previousPrice))
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(previousPrice), 10, RoundingMode.HALF_UP);

        return new PriceChangeResponse(
                resolveDirection(previousPrice, latestPrice),
                rawIncreaseRate.setScale(2, RoundingMode.HALF_UP),
                resolveScore(rawIncreaseRate),
                previousPrice,
                latestPrice,
                previousPriceDate,
                latestPriceDate
        );
    }

    private void validate(
            int previousPrice,
            LocalDate previousPriceDate,
            int latestPrice,
            LocalDate latestPriceDate
    ) {
        if (previousPrice <= 0) {
            throw new IllegalArgumentException("previousPrice must be greater than zero.");
        }
        if (latestPrice <= 0) {
            throw new IllegalArgumentException("latestPrice must be greater than zero.");
        }

        Objects.requireNonNull(previousPriceDate, "previousPriceDate must not be null.");
        Objects.requireNonNull(latestPriceDate, "latestPriceDate must not be null.");
        if (latestPriceDate.isBefore(previousPriceDate)) {
            throw new IllegalArgumentException("latestPriceDate must not be before previousPriceDate.");
        }
    }

    private PriceDirection resolveDirection(int previousPrice, int latestPrice) {
        if (latestPrice > previousPrice) {
            return PriceDirection.UP;
        }
        if (latestPrice < previousPrice) {
            return PriceDirection.DOWN;
        }
        return PriceDirection.FLAT;
    }

    private int resolveScore(BigDecimal increaseRate) {
        if (increaseRate.compareTo(TWENTY_PERCENT) >= 0) {
            return 20;
        }
        if (increaseRate.compareTo(TEN_PERCENT) >= 0) {
            return 15;
        }
        if (increaseRate.compareTo(FIVE_PERCENT) >= 0) {
            return 10;
        }
        return 5;
    }
}
