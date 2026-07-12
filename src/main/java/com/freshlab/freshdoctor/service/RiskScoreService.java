package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.RiskScore;
import com.freshlab.freshdoctor.dto.RiskGrade;
import com.freshlab.freshdoctor.dto.RiskScoreResponse;
import com.freshlab.freshdoctor.dto.RiskScoreUpsertRequest;
import com.freshlab.freshdoctor.repository.RiskScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskScoreService {

    private static final int RAW_SCORE_MAX = 85;
    private static final int MAX_FINAL_SCORE = 100;

    private final RiskScoreRepository riskScoreRepository;
    private final ItemService itemService;

    @Transactional
    public RiskScoreResponse upsert(RiskScoreUpsertRequest request) {
        validate(request);
        itemService.getItem(request.itemCode());

        RiskScore riskScore = riskScoreRepository
                .findByItemCodeAndScoreDate(request.itemCode(), request.scoreDate())
                .orElseGet(RiskScore::new);

        apply(request, riskScore);
        RiskScore saved = riskScoreRepository.save(riskScore);
        return RiskScoreResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public RiskScoreResponse getByDate(String itemCode, LocalDate scoreDate) {
        itemService.getItem(itemCode);
        return riskScoreRepository.findByItemCodeAndScoreDate(itemCode, scoreDate)
                .map(RiskScoreResponse::from)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public RiskScoreResponse getLatest(String itemCode) {
        itemService.getItem(itemCode);
        return riskScoreRepository.findTopByItemCodeOrderByScoreDateDescIdDesc(itemCode)
                .map(RiskScoreResponse::from)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<RiskScoreResponse> getScores(String itemCode, LocalDate startDate, LocalDate endDate) {
        itemService.getItem(itemCode);
        LocalDate resolvedEndDate = endDate == null ? LocalDate.now() : endDate;
        LocalDate resolvedStartDate = startDate == null ? resolvedEndDate.minusDays(29) : startDate;
        return riskScoreRepository.findByItemCodeAndScoreDateBetweenOrderByScoreDateAsc(
                        itemCode,
                        resolvedStartDate,
                        resolvedEndDate
                )
                .stream()
                .map(RiskScoreResponse::from)
                .toList();
    }

    private void apply(RiskScoreUpsertRequest request, RiskScore riskScore) {
        int rawScore = sumScores(
                request.priceIncreaseScore(),
                request.normalYearScore(),
                request.volatilityScore(),
                request.weatherScore(),
                request.newsScore()
        );
        int finalScore = normalize(rawScore);
        RiskGrade riskGrade = resolveRiskGrade(finalScore);

        riskScore.setItemCode(request.itemCode());
        riskScore.setScoreDate(request.scoreDate());
        riskScore.setPriceIncreaseRate(request.priceIncreaseRate());
        riskScore.setPriceIncreaseScore(request.priceIncreaseScore());
        riskScore.setNormalYearComparisonRate(request.normalYearComparisonRate());
        riskScore.setNormalYearScore(request.normalYearScore());
        riskScore.setPriceVolatilityRate(request.priceVolatilityRate());
        riskScore.setVolatilityScore(request.volatilityScore());
        riskScore.setWeatherRiskType(request.weatherRiskType());
        riskScore.setWeatherScore(request.weatherScore());
        riskScore.setWeatherReason(request.weatherReason());
        riskScore.setWeatherBaseDate(request.weatherBaseDate());
        riskScore.setWeatherBaseTime(request.weatherBaseTime());
        riskScore.setNewsRiskType(request.newsRiskType());
        riskScore.setNewsScore(request.newsScore());
        riskScore.setNewsReason(request.newsReason());
        riskScore.setRepresentativeNewsArticleId(request.representativeNewsArticleId());
        riskScore.setRawScore(rawScore);
        riskScore.setFinalScore(finalScore);
        riskScore.setRiskGrade(riskGrade.name());
        riskScore.setUnavailableItems(request.unavailableItems());
        riskScore.setUnavailableReasons(request.unavailableReasons());

        riskScore.setPriceScore(request.priceIncreaseScore());
        riskScore.setYearlyScore(request.normalYearScore());
        riskScore.setSupplyScore(request.newsScore());
        riskScore.setNewsScore(request.newsScore());
        riskScore.setTotalScore(finalScore);
        riskScore.setGrade(riskGrade.name());
        riskScore.setReasons(buildReasons(request));
    }

    private String buildReasons(RiskScoreUpsertRequest request) {
        return String.join("\n", List.of(
                        nullToEmpty(request.weatherReason()),
                        nullToEmpty(request.newsReason()),
                        nullToEmpty(request.unavailableReasons())
                ))
                .trim();
    }

    private int sumScores(Integer... scores) {
        int total = 0;
        for (Integer score : scores) {
            if (score != null) {
                total += score;
            }
        }
        return total;
    }

    private int normalize(int rawScore) {
        if (rawScore <= 0) {
            return 0;
        }
        int normalized = (int) Math.round(rawScore * (double) MAX_FINAL_SCORE / RAW_SCORE_MAX);
        return Math.min(normalized, MAX_FINAL_SCORE);
    }

    private RiskGrade resolveRiskGrade(int finalScore) {
        if (finalScore >= 85) {
            return RiskGrade.SEVERE;
        }
        if (finalScore >= 70) {
            return RiskGrade.ALERT;
        }
        if (finalScore >= 50) {
            return RiskGrade.CAUTION;
        }
        if (finalScore >= 30) {
            return RiskGrade.WATCH;
        }
        return RiskGrade.STABLE;
    }

    private void validate(RiskScoreUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null.");
        }
        if (request.itemCode() == null || request.itemCode().isBlank()) {
            throw new IllegalArgumentException("itemCode must not be blank.");
        }
        if (request.scoreDate() == null) {
            throw new IllegalArgumentException("scoreDate must not be null.");
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
