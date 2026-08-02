package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.dto.ComparisonStatus;
import com.freshlab.freshdoctor.dto.PriceVolatilityResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceVolatilityCalculatorTest {

    private final PriceVolatilityCalculator calculator = new PriceVolatilityCalculator();

    @Test
    void returnsUnavailableWhenValidPriceCountIsLessThanSeven() {
        PriceVolatilityResponse result = calculator.calculate(List.of(10_000, 10_100, 10_200));

        assertThat(result.status()).isEqualTo(ComparisonStatus.UNAVAILABLE);
        assertThat(result.volatilityRate()).isNull();
        assertThat(result.score()).isNull();
        assertThat(result.usedPriceCount()).isEqualTo(3);
        assertThat(result.unavailableReason())
                .isEqualTo("Insufficient valid prices: need 7 recent prices.");
    }

    @Test
    void calculatesZeroScoreWhenVolatilityIsLessThanThreePercent() {
        PriceVolatilityResponse result = calculator.calculate(List.of(
                10_000, 10_100, 10_200, 10_300, 10_400, 10_500, 10_600
        ));

        assertThat(result.status()).isEqualTo(ComparisonStatus.CALCULATED);
        assertThat(result.volatilityRate()).isLessThan(new BigDecimal("3.00"));
        assertThat(result.score()).isZero();
        assertThat(result.usedPriceCount()).isEqualTo(7);
        assertThat(result.unavailableReason()).isNull();
    }

    @Test
    void calculatesFivePointScoreFromThreePercentBoundary() {
        PriceVolatilityResponse result = calculator.calculateFromDailyChangeRates(List.of(
                new BigDecimal("3"),
                new BigDecimal("-3"),
                new BigDecimal("3"),
                new BigDecimal("-3"),
                new BigDecimal("3"),
                new BigDecimal("-3")
        ), 7);

        assertThat(result.volatilityRate()).isEqualByComparingTo("3.00");
        assertThat(result.score()).isEqualTo(5);
    }

    @Test
    void calculatesZeroPointScoreBelowThreePercentBoundary() {
        PriceVolatilityResponse result = calculator.calculateFromDailyChangeRates(List.of(
                new BigDecimal("2.99"),
                new BigDecimal("-2.99"),
                new BigDecimal("2.99"),
                new BigDecimal("-2.99"),
                new BigDecimal("2.99"),
                new BigDecimal("-2.99")
        ));

        assertThat(result.volatilityRate()).isEqualByComparingTo("2.99");
        assertThat(result.score()).isZero();
        assertThat(result.usedPriceCount()).isEqualTo(7);
    }

    @Test
    void calculatesTenPointScoreFromSevenPercentBoundary() {
        PriceVolatilityResponse result = calculator.calculateFromDailyChangeRates(List.of(
                new BigDecimal("7"),
                new BigDecimal("-7"),
                new BigDecimal("7"),
                new BigDecimal("-7"),
                new BigDecimal("7"),
                new BigDecimal("-7")
        ), 7);

        assertThat(result.volatilityRate()).isEqualByComparingTo("7.00");
        assertThat(result.score()).isEqualTo(10);
    }

    @Test
    void calculatesFifteenPointScoreFromFifteenPercentBoundary() {
        PriceVolatilityResponse result = calculator.calculateFromDailyChangeRates(List.of(
                new BigDecimal("15"),
                new BigDecimal("-15"),
                new BigDecimal("15"),
                new BigDecimal("-15"),
                new BigDecimal("15"),
                new BigDecimal("-15")
        ), 7);

        assertThat(result.volatilityRate()).isEqualByComparingTo("15.00");
        assertThat(result.score()).isEqualTo(15);
    }

    @Test
    void usesMostRecentSevenPricesOnly() {
        PriceVolatilityResponse result = calculator.calculate(List.of(
                1_000, 50_000, 10_000, 10_100, 10_200, 10_300, 10_400, 10_500, 10_600
        ));

        assertThat(result.volatilityRate()).isLessThan(new BigDecimal("3.00"));
        assertThat(result.usedPriceCount()).isEqualTo(7);
    }

    @Test
    void rejectsNullListAndInvalidPrices() {
        List<Integer> nullPrices = null;

        assertThatThrownBy(() -> calculator.calculate(nullPrices))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("prices must not be null.");

        assertThatThrownBy(() -> calculator.calculate(
                List.of(10_000, 10_300, 10_000, 0, 10_000, 10_300, 10_000)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("prices must contain only positive values.");

        assertThatThrownBy(() -> calculator.calculateFromDailyChangeRates(
                Arrays.asList(BigDecimal.ONE, null), 3
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dailyChangeRates must not contain null.");
    }
}
