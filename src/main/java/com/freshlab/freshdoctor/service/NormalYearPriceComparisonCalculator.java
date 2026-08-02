package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.dto.ComparisonStatus;
import com.freshlab.freshdoctor.dto.NormalYearPriceComparison;
import com.freshlab.freshdoctor.dto.PriceDirection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class NormalYearPriceComparisonCalculator {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO_PERCENT = BigDecimal.ZERO;
    private static final BigDecimal TEN_PERCENT = BigDecimal.TEN;
    private static final BigDecimal TWENTY_PERCENT = BigDecimal.valueOf(20);

    public NormalYearPriceComparison calculate(Integer currentPrice, Integer normalYearPrice) {
        validateCurrentPrice(currentPrice);

        if (normalYearPrice == null) {
            return new NormalYearPriceComparison(
                    ComparisonStatus.UNAVAILABLE,
                    currentPrice,
                    null,
                    null,
                    null,
                    null
            );
        }

        validateNormalYearPrice(normalYearPrice);

        BigDecimal rawComparisonRate = BigDecimal.valueOf(currentPrice)
                .subtract(BigDecimal.valueOf(normalYearPrice))
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(normalYearPrice), 10, RoundingMode.HALF_UP);

        return new NormalYearPriceComparison(
                ComparisonStatus.CALCULATED,
                currentPrice,
                normalYearPrice,
                rawComparisonRate.setScale(2, RoundingMode.HALF_UP),
                resolveDirection(currentPrice, normalYearPrice),
                resolveScore(rawComparisonRate)
        );
    }

    private void validateCurrentPrice(Integer currentPrice) {
        if (currentPrice == null || currentPrice <= 0) {
            throw new IllegalArgumentException("currentPrice must be greater than zero.");
        }
    }

    private void validateNormalYearPrice(Integer normalYearPrice) {
        if (normalYearPrice <= 0) {
            throw new IllegalArgumentException("normalYearPrice must be greater than zero.");
        }
    }

    private PriceDirection resolveDirection(int currentPrice, int normalYearPrice) {
        if (currentPrice > normalYearPrice) {
            return PriceDirection.UP;
        }
        if (currentPrice < normalYearPrice) {
            return PriceDirection.DOWN;
        }
        return PriceDirection.FLAT;
    }

    private int resolveScore(BigDecimal comparisonRate) {
        if (comparisonRate.compareTo(ZERO_PERCENT) < 0) {
            return 0;
        }
        if (comparisonRate.compareTo(TWENTY_PERCENT) >= 0) {
            return 20;
        }
        if (comparisonRate.compareTo(TEN_PERCENT) > 0) {
            return 15;
        }
        return 10;
    }
}
