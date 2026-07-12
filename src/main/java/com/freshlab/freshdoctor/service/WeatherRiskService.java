package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.domain.WeatherData;
import com.freshlab.freshdoctor.dto.WeatherRiskResponse;
import com.freshlab.freshdoctor.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WeatherRiskService {

    private final ItemService itemService;
    private final WeatherDataRepository weatherDataRepository;
    private final WeatherRiskCalculator weatherRiskCalculator;

    @Transactional(readOnly = true)
    public WeatherRiskResponse calculateRisk(String itemCode) {
        Item item = itemService.getItem(itemCode);
        String region = resolveRegion(item);
        List<WeatherData> weatherData = weatherDataRepository
                .findTop100ByItemCodeAndRegionOrderByForecastDateDescForecastTimeDesc(
                        itemCode,
                        region
                );

        return weatherRiskCalculator.calculate(itemCode, region, weatherData);
    }

    private String resolveRegion(Item item) {
        if (item.getWeatherRegion() != null && !item.getWeatherRegion().isBlank()) {
            return item.getWeatherRegion();
        }
        return item.getOriginRegion();
    }
}
