package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.dto.RiskDashboardResponse;
import com.freshlab.freshdoctor.dto.RiskFactorResponse;
import com.freshlab.freshdoctor.dto.RiskHistoryResponse;
import com.freshlab.freshdoctor.dto.RiskScoreResponse;
import com.freshlab.freshdoctor.dto.TotalRiskCalculationResult;
import com.freshlab.freshdoctor.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskDashboardService {

    private static final int PRICE_INCREASE_MAX_SCORE = 20;
    private static final int NORMAL_YEAR_MAX_SCORE = 20;
    private static final int VOLATILITY_MAX_SCORE = 15;
    private static final int WEATHER_MAX_SCORE = 20;
    private static final int NEWS_MAX_SCORE = 10;

    private final RiskScoreService riskScoreService;
    private final TotalRiskService totalRiskService;
    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public RiskDashboardResponse getLatestRisk(String itemCode) {
        RiskScoreResponse riskScore = riskScoreService.getLatest(itemCode);
        return riskScore == null ? null : toDashboardResponse(riskScore);
    }

    @Transactional(readOnly = true)
    public List<RiskHistoryResponse> getHistory(String itemCode, LocalDate startDate, LocalDate endDate) {
        return riskScoreService.getScores(itemCode, startDate, endDate)
                .stream()
                .map(RiskHistoryResponse::from)
                .toList();
    }

    @Transactional
    public List<TotalRiskCalculationResult> calculateAll(LocalDate scoreDate) {
        return itemRepository.findByActiveTrueOrderByItemNameAsc()
                .stream()
                .map(Item::getItemCode)
                .map(itemCode -> totalRiskService.calculateAndSave(itemCode, scoreDate))
                .toList();
    }

    public RiskDashboardResponse toDashboardResponse(RiskScoreResponse riskScore) {
        return new RiskDashboardResponse(
                riskScore.itemCode(),
                riskScore.scoreDate(),
                riskScore.finalScore(),
                riskScore.riskGrade(),
                List.of(
                        RiskFactorResponse.of("PRICE_INCREASE", riskScore.priceIncreaseScore(), PRICE_INCREASE_MAX_SCORE),
                        RiskFactorResponse.of("NORMAL_YEAR", riskScore.normalYearScore(), NORMAL_YEAR_MAX_SCORE),
                        RiskFactorResponse.of("VOLATILITY", riskScore.volatilityScore(), VOLATILITY_MAX_SCORE),
                        RiskFactorResponse.of("WEATHER", riskScore.weatherScore(), WEATHER_MAX_SCORE),
                        RiskFactorResponse.of("NEWS_SUPPLY", riskScore.newsScore(), NEWS_MAX_SCORE)
                ),
                riskScore.priceIncreaseRate(),
                riskScore.normalYearComparisonRate(),
                riskScore.priceVolatilityRate(),
                riskScore.weatherReason(),
                riskScore.newsReason(),
                riskScore.scoreDate(),
                riskScore.updatedAt(),
                split(riskScore.unavailableItems(), ","),
                split(riskScore.unavailableReasons(), ";")
        );
    }

    private List<String> split(String value, String delimiter) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(delimiter))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
    }
}
