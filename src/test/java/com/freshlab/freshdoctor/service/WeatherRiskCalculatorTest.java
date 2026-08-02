package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.WeatherData;
import com.freshlab.freshdoctor.dto.ComparisonStatus;
import com.freshlab.freshdoctor.dto.WeatherRiskResponse;
import com.freshlab.freshdoctor.dto.WeatherRiskType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeatherRiskCalculatorTest {

    private final WeatherRiskCalculator calculator = new WeatherRiskCalculator();

    @Test
    void returnsUnavailableWhenWeatherDataIsEmpty() {
        WeatherRiskResponse result = calculator.calculate("ITEM-001", "광주", List.of());

        assertThat(result.status()).isEqualTo(ComparisonStatus.UNAVAILABLE);
        assertThat(result.score()).isNull();
        assertThat(result.riskType()).isNull();
        assertThat(result.unavailableReason()).isEqualTo("No weather data available.");
    }

    @Test
    void calculatesZeroScoreWhenThereIsNoWeatherIssue() {
        WeatherRiskResponse result = calculator.calculate("ITEM-001", "광주", List.of(
                weather("TMP", "25", 25.0)
        ));

        assertThat(result.status()).isEqualTo(ComparisonStatus.CALCULATED);
        assertThat(result.riskType()).isEqualTo(WeatherRiskType.NONE);
        assertThat(result.score()).isZero();
        assertThat(result.reason()).isEqualTo("기상 위험 이슈가 없습니다.");
    }

    @Test
    void calculatesRainRiskFromRainProbability() {
        WeatherRiskResponse result = calculator.calculate("ITEM-001", "광주", List.of(
                weather("POP", "60", 60.0)
        ));

        assertThat(result.riskType()).isEqualTo(WeatherRiskType.RAIN);
        assertThat(result.score()).isEqualTo(10);
    }

    @Test
    void calculatesHeatWaveRiskFromTemperature() {
        WeatherRiskResponse result = calculator.calculate("ITEM-001", "광주", List.of(
                weather("TMX", "33", 33.0)
        ));

        assertThat(result.riskType()).isEqualTo(WeatherRiskType.HEAT_WAVE);
        assertThat(result.score()).isEqualTo(10);
    }

    @Test
    void calculatesHeavyRainRiskFromPrecipitationText() {
        WeatherRiskResponse result = calculator.calculate("ITEM-001", "광주", List.of(
                weather("PCP", "30.0mm", null)
        ));

        assertThat(result.riskType()).isEqualTo(WeatherRiskType.HEAVY_RAIN);
        assertThat(result.score()).isEqualTo(20);
    }

    @Test
    void usesHighestRiskOnlyWhenMultipleIssuesExist() {
        WeatherRiskResponse result = calculator.calculate("ITEM-001", "광주", List.of(
                weather("POP", "80", 80.0),
                weather("PCP", "35.0mm", null),
                weather("TMX", "34", 34.0)
        ));

        assertThat(result.riskType()).isEqualTo(WeatherRiskType.HEAVY_RAIN);
        assertThat(result.score()).isEqualTo(20);
    }

    @Test
    void rejectsNullWeatherDataList() {
        List<WeatherData> nullWeatherData = null;

        assertThatThrownBy(() -> calculator.calculate("ITEM-001", "광주", nullWeatherData))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("weatherData must not be null.");
    }

    private WeatherData weather(String category, String valueText, Double valueNumber) {
        WeatherData weatherData = new WeatherData();
        weatherData.setItemCode("ITEM-001");
        weatherData.setRegion("광주");
        weatherData.setBaseDate(LocalDate.of(2026, 7, 12));
        weatherData.setBaseTime("1400");
        weatherData.setForecastDate(LocalDate.of(2026, 7, 12));
        weatherData.setForecastTime("1500");
        weatherData.setCategory(category);
        weatherData.setValueText(valueText);
        weatherData.setValueNumber(valueNumber);
        weatherData.setSource("KMA");
        return weatherData;
    }
}
