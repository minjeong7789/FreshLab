package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.dto.ComparisonStatus;
import com.freshlab.freshdoctor.dto.NormalYearPriceComparison;
import com.freshlab.freshdoctor.dto.PriceDirection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NormalYearPriceComparisonCalculatorTest {

    private final NormalYearPriceComparisonCalculator calculator =
            new NormalYearPriceComparisonCalculator();

    @ParameterizedTest
    @CsvSource({
            "9000, 10000, -10.00, DOWN, 0",
            "10000, 10000, 0.00, FLAT, 10",
            "10999, 10000, 9.99, UP, 10",
            "11000, 10000, 10.00, UP, 10",
            "11001, 10000, 10.01, UP, 15",
            "11999, 10000, 19.99, UP, 15",
            "12000, 10000, 20.00, UP, 20"
    })
    void calculatesRateDirectionAndScore(
            int currentPrice,
            int normalYearPrice,
            String expectedRate,
            PriceDirection expectedDirection,
            int expectedScore
    ) {
        NormalYearPriceComparison result = calculator.calculate(currentPrice, normalYearPrice);

        assertThat(result.status()).isEqualTo(ComparisonStatus.CALCULATED);
        assertThat(result.currentPrice()).isEqualTo(currentPrice);
        assertThat(result.normalYearPrice()).isEqualTo(normalYearPrice);
        assertThat(result.comparisonRate()).isEqualByComparingTo(new BigDecimal(expectedRate));
        assertThat(result.direction()).isEqualTo(expectedDirection);
        assertThat(result.score()).isEqualTo(expectedScore);
    }

    @Test
    void returnsUnavailableWithoutInventingNormalYearPrice() {
        NormalYearPriceComparison result = calculator.calculate(10_000, null);

        assertThat(result.status()).isEqualTo(ComparisonStatus.UNAVAILABLE);
        assertThat(result.currentPrice()).isEqualTo(10_000);
        assertThat(result.normalYearPrice()).isNull();
        assertThat(result.comparisonRate()).isNull();
        assertThat(result.direction()).isNull();
        assertThat(result.score()).isNull();
    }

    @Test
    void usesUnroundedRateForScore() {
        NormalYearPriceComparison result = calculator.calculate(23_999, 20_000);

        assertThat(result.comparisonRate()).isEqualByComparingTo("20.00");
        assertThat(result.score()).isEqualTo(15);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void rejectsInvalidCurrentPrice(Integer currentPrice) {
        assertThatThrownBy(() -> calculator.calculate(currentPrice, 10_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("currentPrice must be greater than zero.");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidNormalYearPrice(int normalYearPrice) {
        assertThatThrownBy(() -> calculator.calculate(10_000, normalYearPrice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("normalYearPrice must be greater than zero.");
    }
}
