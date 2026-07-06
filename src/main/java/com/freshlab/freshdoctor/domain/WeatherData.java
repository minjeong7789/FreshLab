package com.freshlab.freshdoctor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "weather_data",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_weather_item_region_date_time_category",
                columnNames = {"item_code", "region", "forecast_date", "forecast_time", "category"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class WeatherData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false)
    private String itemCode;

    @Column(name = "region", nullable = false)
    private String region;

    @Column(name = "nx")
    private Integer nx;

    @Column(name = "ny")
    private Integer ny;

    @Column(name = "base_date")
    private LocalDate baseDate;

    @Column(name = "base_time", length = 4)
    private String baseTime;

    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    @Column(name = "forecast_time", nullable = false, length = 4)
    private String forecastTime;

    @Column(name = "category", nullable = false, length = 20)
    private String category;

    @Column(name = "value_text", nullable = false)
    private String valueText;

    @Column(name = "value_number")
    private Double valueNumber;

    @Column(name = "source", nullable = false)
    private String source = "KMA";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
