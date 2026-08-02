package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.Alert;
import com.freshlab.freshdoctor.domain.AlertType;
import com.freshlab.freshdoctor.domain.RiskScore;
import com.freshlab.freshdoctor.domain.UserItem;
import com.freshlab.freshdoctor.dto.RiskGrade;
import com.freshlab.freshdoctor.repository.AlertRepository;
import com.freshlab.freshdoctor.repository.UserItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public int createAlerts(RiskSnapshot previous, RiskScore current) {
        List<UserItem> targets = userItemRepository
                .findByItemItemCode(current.getItemCode());
        int createdCount = 0;
        for (UserItem target : targets) {
            for (AlertCandidate candidate : detect(previous, current, target)) {
                if (alertRepository.existsByUserUserIdAndItemCodeAndAlertTypeAndRiskScoreDate(
                        target.getUser().getUserId(), current.getItemCode(), candidate.type(), current.getScoreDate())) {
                    continue;
                }
                Alert savedAlert = alertRepository.save(toAlert(target, previous, current, candidate));
                if (savedAlert != null && savedAlert.getId() != null) {
                    eventPublisher.publishEvent(new com.freshlab.freshdoctor.event.AlertCreatedEvent(savedAlert.getId()));
                }
                createdCount++;
            }
        }
        return createdCount;
    }

    private List<AlertCandidate> detect(RiskSnapshot previous, RiskScore current, UserItem target) {
        List<AlertCandidate> candidates = new ArrayList<>();
        String itemName = target.getItem().getItemName();
        RiskGrade currentGrade = RiskGrade.valueOf(current.getRiskGrade());
        RiskGrade previousGrade = previous == null || previous.grade() == null
                ? null : RiskGrade.valueOf(previous.grade());

        if (previousGrade != null && currentGrade != previousGrade) {
            boolean increased = currentGrade.ordinal() > previousGrade.ordinal();
            candidates.add(new AlertCandidate(
                    increased ? AlertType.GRADE_INCREASE : AlertType.GRADE_DECREASE,
                    itemName + " 위험 등급 " + (increased ? "상승" : "하락"),
                    itemName + "의 위험 등급이 " + gradeLabel(previousGrade)
                            + "에서 " + gradeLabel(currentGrade) + "(으)로 변경되었습니다.",
                    "이전 점수=" + previous.score() + ", 현재 점수=" + current.getFinalScore()));
        }

        if (currentGrade != RiskGrade.SAFE && currentGrade != previousGrade) {
            candidates.add(new AlertCandidate(
                    AlertType.RISK_LEVEL_ENTRY,
                    itemName + " " + gradeLabel(currentGrade) + " 단계 진입",
                    itemName + "이(가) " + gradeLabel(currentGrade) + " 단계에 진입했습니다.",
                    "현재 점수=" + current.getFinalScore() + ", 현재 등급=" + gradeLabel(currentGrade)));
        }

        BigDecimal volatilityThreshold = thresholdOrDefault(target.getPriceVolatilityThreshold());
        if (crossedThreshold(previous == null ? null : previous.priceVolatilityRate(),
                current.getPriceVolatilityRate(), volatilityThreshold)) {
            candidates.add(new AlertCandidate(
                    AlertType.PRICE_VOLATILITY_THRESHOLD,
                    itemName + " 가격 변동성 기준 초과",
                    itemName + "의 가격 변동성이 설정한 기준을 초과했습니다.",
                    "가격 변동성=" + current.getPriceVolatilityRate() + "%, 기준=" + volatilityThreshold + "%"));
        }

        BigDecimal increaseThreshold = thresholdOrDefault(target.getPriceIncreaseThreshold());
        if (crossedThreshold(previous == null ? null : previous.priceIncreaseRate(),
                current.getPriceIncreaseRate(), increaseThreshold)) {
            candidates.add(new AlertCandidate(
                    AlertType.PRICE_INCREASE_THRESHOLD,
                    itemName + " 가격 상승률 기준 초과",
                    itemName + "의 최근 가격 상승률이 설정한 기준을 초과했습니다.",
                    "가격 상승률=" + current.getPriceIncreaseRate() + "%, 기준=" + increaseThreshold + "%"));
        }

        if (isNewSevereIssue(previous == null ? null : previous.weatherScore(),
                previous == null ? null : previous.weatherRiskType(), current.getWeatherScore(),
                current.getWeatherRiskType(), SEVERE_WEATHER_SCORE)) {
            candidates.add(new AlertCandidate(
                    AlertType.SEVERE_WEATHER_ISSUE,
                    itemName + " 중대한 기상 위험 감지",
                    textOrDefault(current.getWeatherReason(), "중대한 기상 위험이 감지되었습니다."),
                    "기상 위험 유형=" + current.getWeatherRiskType() + ", 기상 점수=" + current.getWeatherScore()));
        }

        if (isNewSevereIssue(previous == null ? null : previous.newsScore(),
                previous == null ? null : previous.newsRiskType(), current.getNewsScore(),
                current.getNewsRiskType(), SEVERE_NEWS_SCORE)) {
            candidates.add(new AlertCandidate(
                    AlertType.SEVERE_NEWS_ISSUE,
                    itemName + " 중대한 뉴스·수급 위험 감지",
                    textOrDefault(current.getNewsReason(), "중대한 뉴스 또는 수급 위험이 감지되었습니다."),
                    "뉴스 위험 유형=" + current.getNewsRiskType() + ", 뉴스 점수=" + current.getNewsScore()));
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
        return switch (grade) {
            case SAFE -> "안정";
            case INTEREST -> "관심";
            case CAUTION -> "주의";
            case ALERT -> "경계";
            case CRITICAL -> "심각";
        };
    }

    private String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record AlertCandidate(AlertType type, String title, String description, String evidence) {
    }
}
