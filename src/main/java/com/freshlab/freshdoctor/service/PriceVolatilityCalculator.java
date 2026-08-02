package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.dto.ComparisonStatus;
import com.freshlab.freshdoctor.dto.PriceVolatilityResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Component
public class PriceVolatilityCalculator {

    private static final int REQUIRED_PRICE_COUNT = 7;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal THREE_PERCENT = BigDecimal.valueOf(3);
    private static final BigDecimal SEVEN_PERCENT = BigDecimal.valueOf(7);
    private static final BigDecimal FIFTEEN_PERCENT = BigDecimal.valueOf(15);
    private static final String INSUFFICIENT_PRICE_REASON =
            "Insufficient valid prices: need 7 recent prices.";

    public PriceVolatilityResponse calculate(List<Integer> prices) {
        Objects.requireNonNull(prices, "prices must not be null.");

        if (prices.size() < REQUIRED_PRICE_COUNT) {
            return new PriceVolatilityResponse(
                    ComparisonStatus.UNAVAILABLE,
                    null,
                    null,
                    prices.size(),
                    INSUFFICIENT_PRICE_REASON
            );
        }

        List<Integer> recentPrices = prices.stream()
                .skip(Math.max(0, prices.size() - REQUIRED_PRICE_COUNT))
                .toList();
        validatePrices(recentPrices);

        List<BigDecimal> dailyChangeRates = calculateDailyChangeRates(recentPrices);
        return calculateFromDailyChangeRates(dailyChangeRates, recentPrices.size());
    }

    public PriceVolatilityResponse calculateFromDailyChangeRates(List<BigDecimal> dailyChangeRates) {
        Objects.requireNonNull(dailyChangeRates, "dailyChangeRates must not be null.");
        return calculateFromDailyChangeRates(dailyChangeRates, dailyChangeRates.size() + 1);
    }

    public PriceVolatilityResponse calculateFromDailyChangeRates(
            List<BigDecimal> dailyChangeRates,
            int usedPriceCount
    ) {
        Objects.requireNonNull(dailyChangeRates, "dailyChangeRates must not be null.");
        if (dailyChangeRates.isEmpty()) {
            return new PriceVolatilityResponse(
                    ComparisonStatus.UNAVAILABLE,
                    null,
                    null,
                    usedPriceCount,
                    INSUFFICIENT_PRICE_REASON
            );
        }
        validateRates(dailyChangeRates);

        BigDecimal volatilityRate = calculateStandardDeviation(dailyChangeRates);

        return new PriceVolatilityResponse(
                ComparisonStatus.CALCULATED,
                volatilityRate.setScale(2, RoundingMode.HALF_UP),
                resolveScore(volatilityRate),
                usedPriceCount,
                null
        );
    }

    private List<BigDecimal> calculateDailyChangeRates(List<Integer> prices) {
        return java.util.stream.IntStream.range(1, prices.size())
                .mapToObj(index -> BigDecimal.valueOf(prices.get(index))
                        .subtract(BigDecimal.valueOf(prices.get(index - 1)))
                        .multiply(ONE_HUNDRED)
                        .divide(BigDecimal.valueOf(prices.get(index - 1)), 10, RoundingMode.HALF_UP))
                .toList();
    }

    private BigDecimal calculateStandardDeviation(List<BigDecimal> rates) {
        BigDecimal mean = rates.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(rates.size()), 10, RoundingMode.HALF_UP);

        BigDecimal variance = rates.stream()
                .map(rate -> rate.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(rates.size()), 10, RoundingMode.HALF_UP);

        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }

    private int resolveScore(BigDecimal volatilityRate) {
        if (volatilityRate.compareTo(FIFTEEN_PERCENT) >= 0) {
            return 15;
        }
        if (volatilityRate.compareTo(SEVEN_PERCENT) >= 0) {
            return 10;
        }
        if (volatilityRate.compareTo(THREE_PERCENT) >= 0) {
            return 5;
        }
        return 0;
    }

    private void validatePrices(List<Integer> prices) {
        boolean hasInvalidPrice = prices.stream().anyMatch(price -> price == null || price <= 0);
        if (hasInvalidPrice) {
            throw new IllegalArgumentException("prices must contain only positive values.");
        }
    }

    private void validateRates(List<BigDecimal> rates) {
        boolean hasInvalidRate = rates.stream().anyMatch(Objects::isNull);
        if (hasInvalidRate) {
            throw new IllegalArgumentException("dailyChangeRates must not contain null.");
        }
    }
}
