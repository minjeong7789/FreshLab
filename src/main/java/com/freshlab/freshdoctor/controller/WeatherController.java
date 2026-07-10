package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.dto.WeatherCollectResult;
import com.freshlab.freshdoctor.dto.WeatherResponse;
import com.freshlab.freshdoctor.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @PostMapping("/collect/{itemCode}")
    public WeatherCollectResult collectForecast(
            @PathVariable String itemCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate baseDate,
            @RequestParam(required = false) String baseTime,
            @RequestParam(required = false) Integer nx,
            @RequestParam(required = false) Integer ny,
            @RequestParam(required = false) String region
    ) {
        return weatherService.collectForecast(itemCode, baseDate, baseTime, nx, ny, region);
    }

    @GetMapping("/{itemCode}")
    public List<WeatherResponse> getForecasts(
            @PathVariable String itemCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return weatherService.getForecasts(itemCode, startDate, endDate);
    }
}
