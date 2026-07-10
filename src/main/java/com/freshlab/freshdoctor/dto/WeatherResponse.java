package com.freshlab.freshdoctor.dto;

import com.freshlab.freshdoctor.domain.WeatherData;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WeatherResponse(
        Long id,
        String itemCode,
        String region,
        Integer nx,
        Integer ny,
        LocalDate baseDate,
        String baseTime,
        LocalDate forecastDate,
        String forecastTime,
        String category,
        String valueText,
        Double valueNumber,
        String source,
        LocalDateTime createdAt
) {

    public static WeatherResponse from(WeatherData weatherData) {
        return new WeatherResponse(
                weatherData.getId(),
                weatherData.getItemCode(),
                weatherData.getRegion(),
                weatherData.getNx(),
                weatherData.getNy(),
                weatherData.getBaseDate(),
                weatherData.getBaseTime(),
                weatherData.getForecastDate(),
                weatherData.getForecastTime(),
                weatherData.getCategory(),
                weatherData.getValueText(),
                weatherData.getValueNumber(),
                weatherData.getSource(),
                weatherData.getCreatedAt()
        );
    }
}
