package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.domain.PriceHistory;
import com.freshlab.freshdoctor.dto.ComparisonStatus;
import com.freshlab.freshdoctor.dto.NewsRiskResponse;
import com.freshlab.freshdoctor.dto.NormalYearPriceComparison;
import com.freshlab.freshdoctor.dto.PriceChangeResponse;
import com.freshlab.freshdoctor.dto.PriceVolatilityResponse;
import com.freshlab.freshdoctor.dto.RiskScoreResponse;
import com.freshlab.freshdoctor.dto.RiskScoreUpsertRequest;
import com.freshlab.freshdoctor.dto.TotalRiskCalculationResult;
import com.freshlab.freshdoctor.dto.WeatherRiskResponse;
import com.freshlab.freshdoctor.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TotalRiskService {

    private final ItemService itemService;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PriceIncreaseRateCalculator priceIncreaseRateCalculator;
    private final NormalYearPriceComparisonCalculator normalYearPriceComparisonCalculator;
    private final PriceVolatilityCalculator priceVolatilityCalculator;
    private final WeatherRiskService weatherRiskService;
    private final NewsRiskService newsRiskService;
    private final TotalRiskCalculator totalRiskCalculator;
    private final RiskScoreService riskScoreService;

    @Transactional
    public TotalRiskCalculationResult calculateAndSave(String itemCode, LocalDate scoreDate) {
        Item item = itemService.getItem(itemCode);
        LocalDate resolvedScoreDate = scoreDate == null ? LocalDate.now() : scoreDate;
        List<String> unavailableItems = new ArrayList<>();
        List<String> unavailableReasons = new ArrayList<>();

        List<PriceHistory> recentPrices = priceHistoryRepository
                .findTop60ByItemCodeAndMarketTypeAndKamisRankCodeAndUnitOrderByPriceDateDesc(
                        itemCode,
                        item.getDefaultMarketType(),
                        item.getDefaultRankCode(),
                        item.getDefaultUnit()
                );

        PriceChangeResponse priceChange = calculatePriceChange(recentPrices, unavailableItems, unavailableReasons);
        NormalYearPriceComparison normalYearComparison = calculateNormalYearComparison(
                recentPrices,
                unavailableItems,
                unavailableReasons
        );
        PriceVolatilityResponse volatility = calculateVolatility(recentPrices, unavailableItems, unavailableReasons);
        WeatherRiskResponse weatherRisk = calculateWeatherRisk(itemCode, unavailableItems, unavailableReasons);
        NewsRiskResponse newsRisk = calculateNewsRisk(itemCode, unavailableItems, unavailableReasons);

        LocalDate latestValidDataDate = latestDate(
                latestPriceDate(recentPrices),
                weatherRisk == null ? null : weatherRisk.forecastDate(),
                newsRisk == null ? null : newsRisk.baseDate()
        );

        TotalRiskCalculationResult calculation = totalRiskCalculator.calculate(
                itemCode,
                resolvedScoreDate,
                priceChange == null ? null : priceChange.score(),
                normalYearComparison == null ? null : normalYearComparison.score(),
                volatility == null ? null : volatility.score(),
                weatherRisk == null ? null : weatherRisk.score(),
                newsRisk == null ? null : newsRisk.score(),
                latestValidDataDate,
                unavailableItems,
                unavailableReasons
        );

        RiskScoreResponse savedRiskScore = riskScoreService.upsert(new RiskScoreUpsertRequest(
                itemCode,
                resolvedScoreDate,
                priceChange == null ? null : priceChange.increaseRate(),
                priceChange == null ? null : priceChange.score(),
                normalYearComparison == null ? null : normalYearComparison.comparisonRate(),
                normalYearComparison == null ? null : normalYearComparison.score(),
                volatility == null ? null : volatility.volatilityRate(),
                volatility == null ? null : volatility.score(),
                weatherRisk == null || weatherRisk.riskType() == null ? null : weatherRisk.riskType().name(),
                weatherRisk == null ? null : weatherRisk.score(),
                weatherRisk == null ? null : weatherRisk.reason(),
                weatherRisk == null ? null : weatherRisk.baseDate(),
                weatherRisk == null ? null : weatherRisk.baseTime(),
                newsRisk == null || newsRisk.riskType() == null ? null : newsRisk.riskType().name(),
                newsRisk == null ? null : newsRisk.score(),
                newsRisk == null ? null : newsRisk.reason(),
                newsRisk == null ? null : newsRisk.representativeArticleId(),
                String.join(",", unavailableItems),
                String.join("; ", unavailableReasons)
        ));

        return new TotalRiskCalculationResult(
                calculation.itemCode(),
                calculation.scoreDate(),
                savedRiskScore.rawScore(),
                savedRiskScore.finalScore(),
                calculation.riskGrade(),
                calculation.latestValidDataDate(),
                savedRiskScore,
                calculation.unavailableItems(),
                calculation.unavailableReasons()
        );
    }

    private PriceChangeResponse calculatePriceChange(
            List<PriceHistory> recentPrices,
            List<String> unavailableItems,
            List<String> unavailableReasons
    ) {
        if (recentPrices.size() < 7) {
            unavailableItems.add("priceIncrease");
            unavailableReasons.add("최근 7개 가격 데이터가 부족해 가격 상승률을 계산할 수 없습니다.");
            return null;
        }
        PriceHistory latest = recentPrices.get(0);
        PriceHistory previous = recentPrices.get(6);
        return priceIncreaseRateCalculator.calculate(
                previous.getPrice(),
                previous.getPriceDate(),
                latest.getPrice(),
                latest.getPriceDate()
        );
    }

    private NormalYearPriceComparison calculateNormalYearComparison(
            List<PriceHistory> recentPrices,
            List<String> unavailableItems,
            List<String> unavailableReasons
    ) {
        PriceHistory latest = recentPrices.stream()
                .max(Comparator.comparing(PriceHistory::getPriceDate))
                .orElse(null);
        if (latest == null) {
            unavailableItems.add("normalYear");
            unavailableReasons.add("가격 데이터가 없어 평년 대비율을 계산할 수 없습니다.");
            return null;
        }
        NormalYearPriceComparison comparison = normalYearPriceComparisonCalculator.calculate(
                latest.getPrice(),
                latest.getNormalYearPrice()
        );
        if (comparison.status() == ComparisonStatus.UNAVAILABLE) {
            unavailableItems.add("normalYear");
            unavailableReasons.add("평년 가격 데이터가 없어 평년 대비율을 계산할 수 없습니다.");
        }
        return comparison;
    }

    private PriceVolatilityResponse calculateVolatility(
            List<PriceHistory> recentPrices,
            List<String> unavailableItems,
            List<String> unavailableReasons
    ) {
        List<Integer> prices = recentPrices.stream()
                .sorted(Comparator.comparing(PriceHistory::getPriceDate))
                .map(PriceHistory::getPrice)
                .toList();
        PriceVolatilityResponse volatility = priceVolatilityCalculator.calculate(prices);
        if (volatility.status() == ComparisonStatus.UNAVAILABLE) {
            unavailableItems.add("volatility");
            unavailableReasons.add(volatility.unavailableReason());
        }
        return volatility;
    }

    private WeatherRiskResponse calculateWeatherRisk(
            String itemCode,
            List<String> unavailableItems,
            List<String> unavailableReasons
    ) {
        WeatherRiskResponse weatherRisk = weatherRiskService.calculateRisk(itemCode);
        if (weatherRisk.status() == ComparisonStatus.UNAVAILABLE) {
            unavailableItems.add("weather");
            unavailableReasons.add(weatherRisk.unavailableReason());
        }
        return weatherRisk;
    }

    private NewsRiskResponse calculateNewsRisk(
            String itemCode,
            List<String> unavailableItems,
            List<String> unavailableReasons
    ) {
        NewsRiskResponse newsRisk = newsRiskService.calculateRisk(itemCode);
        if (newsRisk.status() == ComparisonStatus.UNAVAILABLE) {
            unavailableItems.add("news");
            unavailableReasons.add(newsRisk.unavailableReason());
        }
        return newsRisk;
    }

    private LocalDate latestPriceDate(List<PriceHistory> recentPrices) {
        return recentPrices.stream()
                .map(PriceHistory::getPriceDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    private LocalDate latestDate(LocalDate... dates) {
        LocalDate latest = null;
        for (LocalDate date : dates) {
            if (date != null && (latest == null || date.isAfter(latest))) {
                latest = date;
            }
        }
        return latest;
    }
}
