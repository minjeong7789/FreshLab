package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.ActionRecommendation;
import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.domain.RecommendationGenerationType;
import com.freshlab.freshdoctor.domain.RiskScore;
import com.freshlab.freshdoctor.dto.RecommendationInput;
import com.freshlab.freshdoctor.dto.RecommendationResponse;
import com.freshlab.freshdoctor.exception.RecommendationNotFoundException;
import com.freshlab.freshdoctor.exception.RiskScoreNotFoundException;
import com.freshlab.freshdoctor.repository.ActionRecommendationRepository;
import com.freshlab.freshdoctor.repository.RiskScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final String PROMPT_VERSION = "gpt-recommendation-v2";

    private final ActionRecommendationRepository recommendationRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final ItemService itemService;
    private final OpenAiRecommendationClient openAiRecommendationClient;

    @Transactional(readOnly = true)
    public RecommendationResponse getLatest(String itemCode) {
        itemService.getItem(itemCode);
        return recommendationRepository.findTopByItemCodeOrderByUpdatedAtDescIdDesc(itemCode)
                .map(RecommendationResponse::from)
                .orElseThrow(() -> new RecommendationNotFoundException(itemCode));
    }

    @Transactional
    public RecommendationResponse generate(String itemCode) {
        Item item = itemService.getItem(itemCode);
        RiskScore riskScore = riskScoreRepository.findTopByItemCodeOrderByScoreDateDescIdDesc(itemCode)
                .orElseThrow(() -> new RiskScoreNotFoundException(itemCode));

        RecommendationInput input = toInput(item, riskScore);
        String inputHash = sha256(toHashSource(input));

        ActionRecommendation recommendation = recommendationRepository.findByInputHash(inputHash)
                .map(cached -> refreshFallbackRecommendation(cached, input))
                .orElseGet(() -> createRecommendation(item, riskScore, input, inputHash));

        riskScore.setRecommendation(recommendation.getRecommendation());
        return RecommendationResponse.from(recommendation);
    }

    private ActionRecommendation refreshFallbackRecommendation(
            ActionRecommendation cached,
            RecommendationInput input
    ) {
        if (cached.getGenerationType() == RecommendationGenerationType.GPT) {
            return cached;
        }

        try {
            String content = openAiRecommendationClient.generate(input);
            cached.setRecommendation(content);
            cached.setGenerationType(RecommendationGenerationType.GPT);
            return recommendationRepository.save(cached);
        } catch (Exception ignored) {
            return cached;
        }
    }

    private ActionRecommendation createRecommendation(
            Item item,
            RiskScore riskScore,
            RecommendationInput input,
            String inputHash
    ) {
        RecommendationGenerationType generationType = RecommendationGenerationType.GPT;
        String content;
        try {
            content = openAiRecommendationClient.generate(input);
        } catch (Exception ignored) {
            generationType = RecommendationGenerationType.FALLBACK;
            content = fallbackMessage(input);
        }

        ActionRecommendation recommendation = new ActionRecommendation();
        recommendation.setItemCode(item.getItemCode());
        recommendation.setItemName(item.getItemName());
        recommendation.setRiskScoreId(riskScore.getId());
        recommendation.setInputHash(inputHash);
        recommendation.setRiskGrade(input.riskGrade());
        recommendation.setFinalScore(input.finalScore());
        recommendation.setPriceIncreaseRate(input.priceIncreaseRate());
        recommendation.setWeatherIssue(input.weatherIssue());
        recommendation.setNewsIssue(input.newsIssue());
        recommendation.setRecommendation(content);
        recommendation.setGenerationType(generationType);
        return recommendationRepository.save(recommendation);
    }

    private RecommendationInput toInput(Item item, RiskScore riskScore) {
        return new RecommendationInput(
                item.getItemCode(),
                item.getItemName(),
                riskScore.getRiskGrade(),
                riskScore.getFinalScore(),
                riskScore.getPriceIncreaseRate(),
                riskScore.getNormalYearComparisonRate(),
                riskScore.getPriceVolatilityRate(),
                riskScore.getWeatherReason(),
                riskScore.getNewsReason()
        );
    }

    private String fallbackMessage(RecommendationInput input) {
        String itemName = input.itemName();
        String grade = input.riskGrade() == null ? "STABLE" : input.riskGrade();
        boolean hasWeatherIssue = input.weatherIssue() != null && !input.weatherIssue().isBlank();
        boolean hasNewsIssue = input.newsIssue() != null && !input.newsIssue().isBlank();

        if ("SEVERE".equals(grade) || "ALERT".equals(grade)) {
            return "현재 " + itemName + " 발주 위험이 높으니 당일 필요 물량 위주로 줄이고 대체 품목을 함께 확인하세요. "
                    + fallbackIssueSentence(hasWeatherIssue, hasNewsIssue);
        }
        if ("CAUTION".equals(grade)) {
            return "현재 " + itemName + " 가격과 수급 변화를 한 번 더 확인한 뒤 평소보다 보수적으로 발주하세요. "
                    + fallbackIssueSentence(hasWeatherIssue, hasNewsIssue);
        }
        if ("WATCH".equals(grade)) {
            return "현재 " + itemName + "은 큰 위험은 아니지만 최근 변동 신호가 있으니 다음 발주 전 가격을 재확인하세요.";
        }
        if (hasWeatherIssue || hasNewsIssue) {
            return "현재 " + itemName + " 위험도는 낮지만 감지된 이슈가 있으니 평소 발주 기준을 유지하되 다음 발주 전 한 번 더 확인하세요.";
        }
        return "현재 " + itemName + " 위험도는 낮아 평소 발주 기준을 유지해도 됩니다.";
    }

    private String fallbackIssueSentence(boolean hasWeatherIssue, boolean hasNewsIssue) {
        if (hasWeatherIssue && hasNewsIssue) {
            return "기상과 뉴스 이슈가 함께 감지되어 하루 단위로 재확인하는 것이 좋습니다.";
        }
        if (hasWeatherIssue) {
            return "기상 이슈가 있어 산지 상황을 하루 단위로 확인하는 것이 좋습니다.";
        }
        if (hasNewsIssue) {
            return "뉴스/수급 이슈가 있어 출하 상황을 추가 확인하는 것이 좋습니다.";
        }
        return "가격 데이터 중심의 위험 신호이므로 최근 가격 추세를 계속 확인하세요.";
    }

    private String fallback(RecommendationInput input) {
        String itemName = input.itemName();
        String grade = input.riskGrade() == null ? "STABLE" : input.riskGrade();
        boolean hasWeatherIssue = input.weatherIssue() != null && !input.weatherIssue().isBlank();
        boolean hasNewsIssue = input.newsIssue() != null && !input.newsIssue().isBlank();

        if ("SEVERE".equals(grade) || "ALERT".equals(grade)) {
            return itemName + "은 현재 발주 위험이 높으니 당일 필요 물량 위주로 줄이고 대체 품목을 함께 확인하세요. "
                    + issueSentence(hasWeatherIssue, hasNewsIssue);
        }
        if ("CAUTION".equals(grade)) {
            return itemName + "은 가격과 수급 변화를 한 번 더 확인한 뒤 평소보다 보수적으로 발주하세요. "
                    + issueSentence(hasWeatherIssue, hasNewsIssue);
        }
        if ("WATCH".equals(grade)) {
            return itemName + "은 큰 위험은 아니지만 최근 변동 신호가 있으니 다음 발주 전 가격을 재확인하세요.";
        }
        return itemName + "은 현재 위험도가 낮아 평소 발주 기준을 유지해도 됩니다.";
    }

    private String issueSentence(boolean hasWeatherIssue, boolean hasNewsIssue) {
        if (hasWeatherIssue && hasNewsIssue) {
            return "기상과 뉴스 이슈가 함께 감지되어 하루 단위로 재확인하는 것이 좋습니다.";
        }
        if (hasWeatherIssue) {
            return "기상 이슈가 있어 산지 상황을 하루 단위로 확인하는 것이 좋습니다.";
        }
        if (hasNewsIssue) {
            return "뉴스/수급 이슈가 있어 출하 상황을 추가 확인하는 것이 좋습니다.";
        }
        return "가격 데이터 중심의 위험 신호이므로 최근 가격 추세를 계속 확인하세요.";
    }

    private String toHashSource(RecommendationInput input) {
        return new StringJoiner("|")
                .add(PROMPT_VERSION)
                .add(value(input.itemCode()))
                .add(value(input.itemName()))
                .add(value(input.riskGrade()))
                .add(value(input.finalScore()))
                .add(value(input.priceIncreaseRate()))
                .add(value(input.normalYearComparisonRate()))
                .add(value(input.priceVolatilityRate()))
                .add(value(input.weatherIssue()))
                .add(value(input.newsIssue()))
                .toString();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create recommendation input hash.", ex);
        }
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
