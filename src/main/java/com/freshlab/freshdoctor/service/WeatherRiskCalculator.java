package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.WeatherData;
import com.freshlab.freshdoctor.dto.ComparisonStatus;
import com.freshlab.freshdoctor.dto.WeatherRiskResponse;
import com.freshlab.freshdoctor.dto.WeatherRiskType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WeatherRiskCalculator {

    private static final int RAIN_SCORE = 10;
    private static final int HEAT_WAVE_SCORE = 10;
    private static final int SEVERE_SCORE = 20;
    private static final double RAIN_PROBABILITY_THRESHOLD = 60.0;
    private static final double HEAT_WAVE_TEMPERATURE_THRESHOLD = 33.0;
    private static final double HEAVY_RAIN_THRESHOLD = 30.0;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");

    public WeatherRiskResponse calculate(String itemCode, String region, List<WeatherData> weatherData) {
        Objects.requireNonNull(weatherData, "weatherData must not be null.");

        if (weatherData.isEmpty()) {
            return unavailable(itemCode, region, "No weather data available.");
        }

        WeatherRiskCandidate highestRisk = weatherData.stream()
                .map(this::toRiskCandidate)
                .max(Comparator
                        .comparingInt(WeatherRiskCandidate::score)
                        .thenComparing(WeatherRiskCandidate::forecastDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(WeatherRiskCandidate::forecastTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseGet(() -> WeatherRiskCandidate.none(weatherData.get(0)));

        return new WeatherRiskResponse(
                ComparisonStatus.CALCULATED,
                itemCode,
                region,
                highestRisk.riskType(),
                highestRisk.score(),
                highestRisk.reason(),
                highestRisk.baseDate(),
                highestRisk.baseTime(),
                highestRisk.forecastDate(),
                highestRisk.forecastTime(),
                highestRisk.source(),
                null
        );
    }

    private WeatherRiskCandidate toRiskCandidate(WeatherData weatherData) {
        String category = weatherData.getCategory();
        Double numericValue = resolveNumber(weatherData);
        String valueText = weatherData.getValueText();

        if (isTyphoonSignal(valueText)) {
            return WeatherRiskCandidate.of(
                    weatherData,
                    WeatherRiskType.TYPHOON,
                    SEVERE_SCORE,
                    "태풍 관련 기상 신호가 감지되었습니다."
            );
        }
        if ("PCP".equals(category) && numericValue != null && numericValue >= HEAVY_RAIN_THRESHOLD) {
            return WeatherRiskCandidate.of(
                    weatherData,
                    WeatherRiskType.HEAVY_RAIN,
                    SEVERE_SCORE,
                    "시간당 강수량이 " + format(numericValue) + "mm로 호우 위험 기준 이상입니다."
            );
        }
        if (isRainCategory(category, valueText, numericValue)) {
            return WeatherRiskCandidate.of(
                    weatherData,
                    WeatherRiskType.RAIN,
                    RAIN_SCORE,
                    "비 또는 높은 강수확률이 예보되었습니다."
            );
        }
        if (isHeatWaveCategory(category, numericValue)) {
            return WeatherRiskCandidate.of(
                    weatherData,
                    WeatherRiskType.HEAT_WAVE,
                    HEAT_WAVE_SCORE,
                    "예상 기온이 " + format(numericValue) + "도로 폭염 주의 기준 이상입니다."
            );
        }
        return WeatherRiskCandidate.none(weatherData);
    }

    private boolean isRainCategory(String category, String valueText, Double numericValue) {
        if ("POP".equals(category) && numericValue != null && numericValue >= RAIN_PROBABILITY_THRESHOLD) {
            return true;
        }
        if ("PTY".equals(category) && numericValue != null) {
            return numericValue > 0;
        }
        return "PCP".equals(category) && numericValue != null && numericValue > 0;
    }

    private boolean isHeatWaveCategory(String category, Double numericValue) {
        if (!"TMP".equals(category) && !"TMX".equals(category)) {
            return false;
        }
        return numericValue != null && numericValue >= HEAT_WAVE_TEMPERATURE_THRESHOLD;
    }

    private boolean isTyphoonSignal(String valueText) {
        return valueText != null && valueText.contains("태풍");
    }

    private Double resolveNumber(WeatherData weatherData) {
        if (weatherData.getValueNumber() != null) {
            return weatherData.getValueNumber();
        }
        return extractNumber(weatherData.getValueText()).orElse(null);
    }

    private Optional<Double> extractNumber(String valueText) {
        if (valueText == null || valueText.isBlank() || valueText.contains("없음")) {
            return Optional.empty();
        }
        Matcher matcher = NUMBER_PATTERN.matcher(valueText);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Double.parseDouble(matcher.group()));
    }

    private String format(Double value) {
        if (value == null) {
            return "";
        }
        if (value % 1 == 0) {
            return String.valueOf(value.intValue());
        }
        return String.valueOf(value);
    }

    private WeatherRiskResponse unavailable(String itemCode, String region, String reason) {
        return new WeatherRiskResponse(
                ComparisonStatus.UNAVAILABLE,
                itemCode,
                region,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                reason
        );
    }

    private record WeatherRiskCandidate(
            WeatherRiskType riskType,
            int score,
            String reason,
            LocalDate baseDate,
            String baseTime,
            LocalDate forecastDate,
            String forecastTime,
            String source
    ) {

        private static WeatherRiskCandidate of(
                WeatherData weatherData,
                WeatherRiskType riskType,
                int score,
                String reason
        ) {
            return new WeatherRiskCandidate(
                    riskType,
                    score,
                    reason,
                    weatherData.getBaseDate(),
                    weatherData.getBaseTime(),
                    weatherData.getForecastDate(),
                    weatherData.getForecastTime(),
                    weatherData.getSource()
            );
        }

        private static WeatherRiskCandidate none(WeatherData weatherData) {
            return of(weatherData, WeatherRiskType.NONE, 0, "기상 위험 이슈가 없습니다.");
        }
    }
}
