package com.freshlab.freshdoctor.repository;

import com.freshlab.freshdoctor.domain.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {

    List<WeatherData> findByItemCodeAndForecastDateBetweenOrderByForecastDateAscForecastTimeAsc(
            String itemCode,
            LocalDate startDate,
            LocalDate endDate
    );

    List<WeatherData> findTop100ByItemCodeOrderByForecastDateDescForecastTimeDesc(String itemCode);

    Optional<WeatherData> findByItemCodeAndRegionAndForecastDateAndForecastTimeAndCategory(
            String itemCode,
            String region,
            LocalDate forecastDate,
            String forecastTime,
            String category
    );
}
