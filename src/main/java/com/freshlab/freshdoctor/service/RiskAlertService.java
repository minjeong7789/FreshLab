package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.Alert;
import com.freshlab.freshdoctor.domain.AlertType;
import com.freshlab.freshdoctor.domain.RiskScore;
import com.freshlab.freshdoctor.domain.UserItem;
import com.freshlab.freshdoctor.dto.RiskGrade;
import com.freshlab.freshdoctor.repository.AlertRepository;
import com.freshlab.freshdoctor.repository.UserItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RiskAlertService {

    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("10.00");
    private static final int SEVERE_WEATHER_SCORE = 20;
    private static final int SEVERE_NEWS_SCORE = 10;

    private final AlertRepository alertRepository;
    private final UserItemRepository userItemRepository;

    @Transactional
    public int createAlerts(RiskSnapshot previous, RiskScore current) {
        List<UserItem> targets = userItemRepository
                .findByItemItemCodeAndNotificationEnabledTrue(current.getItemCode());
        int createdCount = 0;
        for (UserItem target : targets) {
            for (AlertCandidate candidate : detect(previous, current, target)) {
                if (alertRepository.existsByUserUserIdAndItemCodeAndAlertTypeAndRiskScoreDate(
                        target.getUser().getUserId(), current.getItemCode(), candidate.type(), current.getScoreDate())) {
                    continue;
                }
                alertRepository.save(toAlert(target, previous, current, candidate));
                createdCount++;
            }
        }
        return createdCount;
    }

    private List<AlertCandidate> detect(RiskSnapshot previous, RiskScore current, UserItem target) {
        List<AlertCandidate> candidates = new ArrayList<>();
        RiskGrade currentGrade = RiskGrade.valueOf(current.getRiskGrade());
        RiskGrade previousGrade = previous == null || previous.grade() == null
                ? null : RiskGrade.valueOf(previous.grade());

        if (previousGrade != null && currentGrade != previousGrade) {
            boolean increased = currentGrade.ordinal() > previousGrade.ordinal();
            candidates.add(new AlertCandidate(
                    increased ? AlertType.GRADE_INCREASE : AlertType.GRADE_DECREASE,
                    increased ? "Risk grade increased" : "Risk grade decreased",
                    "Risk grade changed from " + gradeLabel(previousGrade) + " to " + gradeLabel(currentGrade) + ".",
                    "previousScore=" + previous.score() + ", currentScore=" + current.getFinalScore()));
        }

        if (currentGrade != RiskGrade.SAFE && currentGrade != previousGrade) {
            candidates.add(new AlertCandidate(
                    AlertType.RISK_LEVEL_ENTRY,
                    "Entered the " + gradeLabel(currentGrade) + " risk level",
                    target.getItem().getItemName() + " entered the " + gradeLabel(currentGrade) + " risk level.",
                    "currentScore=" + current.getFinalScore() + ", currentGrade=" + currentGrade.name()));
        }

        BigDecimal volatilityThreshold = thresholdOrDefault(target.getPriceVolatilityThreshold());
        if (crossedThreshold(previous == null ? null : previous.priceVolatilityRate(),
                current.getPriceVolatilityRate(), volatilityThreshold)) {
            candidates.add(new AlertCandidate(
                    AlertType.PRICE_VOLATILITY_THRESHOLD,
                    "Price volatility threshold exceeded",
                    "Price volatility exceeded the configured threshold.",
                    "volatility=" + current.getPriceVolatilityRate() + "%, threshold=" + volatilityThreshold + "%"));
        }

        BigDecimal increaseThreshold = thresholdOrDefault(target.getPriceIncreaseThreshold());
        if (crossedThreshold(previous == null ? null : previous.priceIncreaseRate(),
                current.getPriceIncreaseRate(), increaseThreshold)) {
            candidates.add(new AlertCandidate(
                    AlertType.PRICE_INCREASE_THRESHOLD,
                    "Price increase threshold exceeded",
                    "The latest price increase rate exceeded the configured threshold.",
                    "increaseRate=" + current.getPriceIncreaseRate() + "%, threshold=" + increaseThreshold + "%"));
        }

        if (isNewSevereIssue(previous == null ? null : previous.weatherScore(),
                previous == null ? null : previous.weatherRiskType(), current.getWeatherScore(),
                current.getWeatherRiskType(), SEVERE_WEATHER_SCORE)) {
            candidates.add(new AlertCandidate(
                    AlertType.SEVERE_WEATHER_ISSUE,
                    "Severe weather issue detected",
                    textOrDefault(current.getWeatherReason(), "A severe weather risk was detected."),
                    "type=" + current.getWeatherRiskType() + ", weatherScore=" + current.getWeatherScore()));
        }

        if (isNewSevereIssue(previous == null ? null : previous.newsScore(),
                previous == null ? null : previous.newsRiskType(), current.getNewsScore(),
                current.getNewsRiskType(), SEVERE_NEWS_SCORE)) {
            candidates.add(new AlertCandidate(
                    AlertType.SEVERE_NEWS_ISSUE,
                    "Severe news or supply issue detected",
                    textOrDefault(current.getNewsReason(), "A severe news or supply risk was detected."),
                    "type=" + current.getNewsRiskType() + ", newsScore=" + current.getNewsScore()));
        }
        return candidates;
    }

    private Alert toAlert(UserItem target, RiskSnapshot previous, RiskScore current, AlertCandidate candidate) {
        Alert alert = new Alert();
        alert.setUser(target.getUser());
        alert.setItemCode(current.getItemCode());
        alert.setItemName(target.getItem().getItemName());
        alert.setAlertType(candidate.type());
        alert.setPreviousScore(previous == null ? null : previous.score());
        alert.setPreviousGrade(previous == null ? null : previous.grade());
        alert.setCurrentScore(current.getFinalScore());
        alert.setCurrentGrade(current.getRiskGrade());
        alert.setTitle(candidate.title());
        alert.setDescription(candidate.description());
        alert.setEvidence(candidate.evidence());
        alert.setRiskScoreDate(current.getScoreDate());
        alert.setOccurredAt(LocalDateTime.now());
        alert.setIsRead(false);
        return alert;
    }

    private boolean crossedThreshold(BigDecimal previous, BigDecimal current, BigDecimal threshold) {
        return current != null && current.compareTo(threshold) > 0
                && (previous == null || previous.compareTo(threshold) <= 0);
    }

    private boolean isNewSevereIssue(Integer previousScore, String previousType,
                                     Integer currentScore, String currentType, int threshold) {
        if (currentScore == null || currentScore < threshold || currentType == null || "NONE".equals(currentType)) {
            return false;
        }
        return previousScore == null || previousScore < threshold || !Objects.equals(previousType, currentType);
    }

    private BigDecimal thresholdOrDefault(BigDecimal threshold) {
        return threshold == null ? DEFAULT_THRESHOLD : threshold;
    }

    private String gradeLabel(RiskGrade grade) {
        return grade.name().toLowerCase();
    }

    private String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record AlertCandidate(AlertType type, String title, String description, String evidence) {
    }
}
