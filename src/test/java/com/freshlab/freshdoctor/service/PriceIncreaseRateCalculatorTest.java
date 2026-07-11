package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.dto.PriceChangeResponse;
import com.freshlab.freshdoctor.dto.PriceDirection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceIncreaseRateCalculatorTest {

    private static final LocalDate PREVIOUS_DATE = LocalDate.of(2026, 7, 1);
    private static final LocalDate LATEST_DATE = LocalDate.of(2026, 7, 10);
    private final PriceIncreaseRateCalculator calculator = new PriceIncreaseRateCalculator();

    @ParameterizedTest
    @CsvSource({
            "10000, 10499, 4.99, UP, 5",
            "10000, 10500, 5.00, UP, 10",
            "10000, 10999, 9.99, UP, 10",
            "10000, 11000, 10.00, UP, 15",
            "10000, 11999, 19.99, UP, 15",
            "10000, 12000, 20.00, UP, 20",
            "10000, 9000, -10.00, DOWN, 5",
            "10000, 10000, 0.00, FLAT, 5"
    })
    void calculatesDirectionRateAndScore(
            int previousPrice,
            int latestPrice,
            String expectedRate,
            PriceDirection expectedDirection,
            int expectedScore
    ) {
        PriceChangeResponse result = calculator.calculate(
                previousPrice,
                PREVIOUS_DATE,
                latestPrice,
                LATEST_DATE
        );

        assertThat(result.direction()).isEqualTo(expectedDirection);
        assertThat(result.increaseRate()).isEqualByComparingTo(new BigDecimal(expectedRate));
        assertThat(result.score()).isEqualTo(expectedScore);
        assertThat(result.previousPrice()).isEqualTo(previousPrice);
        assertThat(result.latestPrice()).isEqualTo(latestPrice);
        assertThat(result.previousPriceDate()).isEqualTo(PREVIOUS_DATE);
        assertThat(result.latestPriceDate()).isEqualTo(LATEST_DATE);
    }

    @Test
    void usesUnroundedRateForScore() {
        PriceChangeResponse result = calculator.calculate(20000, PREVIOUS_DATE, 20999, LATEST_DATE);

        assertThat(result.increaseRate()).isEqualByComparingTo("5.00");
        assertThat(result.score()).isEqualTo(5);
    }

    @Test
    void rejectsInvalidPricesAndDateOrder() {
        assertThatThrownBy(() -> calculator.calculate(0, PREVIOUS_DATE, 10000, LATEST_DATE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(10000, PREVIOUS_DATE, 0, LATEST_DATE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(10000, LATEST_DATE, 11000, PREVIOUS_DATE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
