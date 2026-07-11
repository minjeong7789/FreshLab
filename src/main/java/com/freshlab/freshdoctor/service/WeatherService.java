package com.freshlab.freshdoctor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.domain.WeatherData;
import com.freshlab.freshdoctor.dto.WeatherCollectResult;
import com.freshlab.freshdoctor.dto.WeatherResponse;
import com.freshlab.freshdoctor.repository.ItemRepository;
import com.freshlab.freshdoctor.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private static final String SOURCE = "KMA";
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int[] FORECAST_BASE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final ItemRepository itemRepository;
    private final WeatherDataRepository weatherDataRepository;

    @Value("${weather.api.url:https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0}")
    private String baseUrl;

    @Value("${weather.api.key}")
    private String serviceKey;

    @Transactional
    public WeatherCollectResult collectForecast(
            String itemCode,
            LocalDate baseDate,
            String baseTime,
            Integer nx,
            Integer ny,
            String region
    ) {
        String resultRegion = region;

        try {
            Item item = itemRepository.findById(itemCode).orElse(null);
            int resolvedNx = resolveNx(item, nx);
            int resolvedNy = resolveNy(item, ny);
            String resolvedRegion = resolveRegion(item, region);
            resultRegion = resolvedRegion;

            ForecastBase latestBase = latestForecastBase();
            LocalDate resolvedBaseDate = baseDate == null ? latestBase.date() : baseDate;
            String resolvedBaseTime = isBlank(baseTime) ? latestBase.time() : baseTime;

            JsonNode root = requestForecast(resolvedBaseDate, resolvedBaseTime, resolvedNx, resolvedNy);
            List<WeatherRow> rows = parseRows(root, itemCode, resolvedRegion, resolvedNx, resolvedNy, resolvedBaseDate, resolvedBaseTime);

            int savedCount = 0;
            for (WeatherRow row : rows) {
                saveOrUpdate(row);
                savedCount++;
            }

            return new WeatherCollectResult(itemCode, resolvedRegion, rows.size(), savedCount, "Weather forecast collection completed.");
        } catch (Exception ex) {
            return new WeatherCollectResult(itemCode, resultRegion, 0, 0, "Weather forecast collection failed: " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<WeatherResponse> getForecasts(String itemCode, LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedEndDate = endDate == null ? LocalDate.now().plusDays(3) : endDate;
        LocalDate resolvedStartDate = startDate == null ? LocalDate.now() : startDate;

        return weatherDataRepository
                .findByItemCodeAndForecastDateBetweenOrderByForecastDateAscForecastTimeAsc(
                        itemCode,
                        resolvedStartDate,
                        resolvedEndDate
                )
                .stream()
                .map(WeatherResponse::from)
                .toList();
    }

    private JsonNode requestForecast(LocalDate baseDate, String baseTime, int nx, int ny) throws Exception {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl + "/getVilageFcst")
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 1000)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate.format(BASIC_DATE))
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .build(true)
                .toUri();

        String body = webClientBuilder.build()
                .get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(
                        Retry.backoff(2, Duration.ofSeconds(1))
                                .filter(this::isRetryableWeatherError)
                )
                .block(Duration.ofSeconds(15));

        if (isBlank(body)) {
            throw new IllegalStateException("Empty weather response.");
        }

        JsonNode root = objectMapper.readTree(body);
        validateWeatherResponse(root);
        return root;
    }

    private void validateWeatherResponse(JsonNode root) {
        JsonNode header = root.path("response").path("header");
        String resultCode = header.path("resultCode").asText("");
        String resultMessage = header.path("resultMsg").asText("");

        if (!"00".equals(resultCode)) {
            throw new IllegalStateException(
                    "KMA API error: code=" + resultCode + ", message=" + resultMessage
            );
        }
    }

    private boolean isRetryableWeatherError(Throwable throwable) {
        return throwable instanceof WebClientResponseException exception
                && exception.getStatusCode().is5xxServerError();
    }

    private List<WeatherRow> parseRows(
            JsonNode root,
            String itemCode,
            String region,
            int nx,
            int ny,
            LocalDate baseDate,
            String baseTime
    ) {
        List<WeatherRow> rows = new ArrayList<>();
        JsonNode items = root.path("response").path("body").path("items").path("item");
        if (!items.isArray()) {
            return rows;
        }

        for (JsonNode item : items) {
            LocalDate forecastDate = parseBasicDate(item.path("fcstDate").asText(null));
            String forecastTime = item.path("fcstTime").asText(null);
            String category = item.path("category").asText(null);
            String value = item.path("fcstValue").asText(null);

            if (forecastDate == null || isBlank(forecastTime) || isBlank(category) || value == null) {
                continue;
            }

            rows.add(new WeatherRow(
                    itemCode,
                    region,
                    nx,
                    ny,
                    baseDate,
                    baseTime,
                    forecastDate,
                    forecastTime,
                    category,
                    value,
                    parseDouble(value)
            ));
        }
        return rows;
    }

    private void saveOrUpdate(WeatherRow row) {
        WeatherData weatherData = weatherDataRepository
                .findByItemCodeAndRegionAndForecastDateAndForecastTimeAndCategory(
                        row.itemCode(),
                        row.region(),
                        row.forecastDate(),
                        row.forecastTime(),
                        row.category()
                )
                .orElseGet(WeatherData::new);

        weatherData.setItemCode(row.itemCode());
        weatherData.setRegion(row.region());
        weatherData.setNx(row.nx());
        weatherData.setNy(row.ny());
        weatherData.setBaseDate(row.baseDate());
        weatherData.setBaseTime(row.baseTime());
        weatherData.setForecastDate(row.forecastDate());
        weatherData.setForecastTime(row.forecastTime());
        weatherData.setCategory(row.category());
        weatherData.setValueText(row.valueText());
        weatherData.setValueNumber(row.valueNumber());
        weatherData.setSource(SOURCE);

        weatherDataRepository.save(weatherData);
    }

    private int resolveNx(Item item, Integer nx) {
        if (nx != null) {
            return nx;
        }
        if (item != null && item.getWeatherNx() != null) {
            return item.getWeatherNx();
        }
        return 58;
    }

    private int resolveNy(Item item, Integer ny) {
        if (ny != null) {
            return ny;
        }
        if (item != null && item.getWeatherNy() != null) {
            return item.getWeatherNy();
        }
        return 74;
    }

    private String resolveRegion(Item item, String region) {
        if (!isBlank(region)) {
            return region;
        }
        if (item != null && !isBlank(item.getWeatherRegion())) {
            return item.getWeatherRegion();
        }
        if (item != null && !isBlank(item.getOriginRegion())) {
            return item.getOriginRegion();
        }
        return "광주";
    }

    private ForecastBase latestForecastBase() {
        LocalDateTime availableAt = LocalDateTime.now().minusMinutes(15);

        for (int i = FORECAST_BASE_HOURS.length - 1; i >= 0; i--) {
            int baseHour = FORECAST_BASE_HOURS[i];
            if (availableAt.getHour() >= baseHour) {
                return new ForecastBase(
                        availableAt.toLocalDate(),
                        String.format("%02d00", baseHour)
                );
            }
        }

        return new ForecastBase(availableAt.toLocalDate().minusDays(1), "2300");
    }

    private LocalDate parseBasicDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value, BASIC_DATE);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record WeatherRow(
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
            Double valueNumber
    ) {
    }

    private record ForecastBase(
            LocalDate date,
            String time
    ) {
    }
}
