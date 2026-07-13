package com.freshlab.freshdoctor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshlab.freshdoctor.dto.RecommendationInput;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiRecommendationClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;

    public String generate(RecommendationInput input) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is empty.");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.3,
                "max_tokens", 120,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", "너는 식자재 발주 위험도를 보고 점주에게 실무적인 행동 추천을 한국어 1~2문장으로 작성한다. 과장하지 말고 바로 실행 가능한 문장만 답한다. 대체 품목을 언급할 때 현재 품목명은 절대 대체 품목으로 다시 말하지 않는다."
                        ),
                        Map.of(
                                "role", "user",
                                "content", buildPrompt(input)
                        )
                )
        );

        String response = webClientBuilder.build()
                .post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode root = objectMapper.readTree(response);
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        if (content.isBlank()) {
            throw new IllegalStateException("OpenAI response has no recommendation content.");
        }
        return limitSentences(content);
    }

    private String buildPrompt(RecommendationInput input) {
        return String.join("\n",
                "품목: " + input.itemName() + "(" + input.itemCode() + ")",
                "위험 등급: " + value(input.riskGrade()),
                "최종 위험 점수: " + value(input.finalScore()),
                "가격 상승률: " + value(input.priceIncreaseRate()) + "%",
                "평년 대비율: " + value(input.normalYearComparisonRate()) + "%",
                "가격 변동성: " + value(input.priceVolatilityRate()) + "%",
                "기상 이슈: " + value(input.weatherIssue()),
                "뉴스/수급 이슈: " + value(input.newsIssue()),
                "요구사항: 발주량 조절, 대체 품목 검토, 모니터링 중 필요한 행동을 1~2문장으로 추천.",
                "금지사항: 현재 품목명 '" + input.itemName() + "'은 대체 품목으로 추천하지 말 것. 문장은 최대 2개까지만 작성할 것."
        );
    }

    private String limitSentences(String value) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        String[] splitBySpace = normalized.split("(?<=[.!?。！？])\\s+");
        if (splitBySpace.length >= 2) {
            return (splitBySpace[0] + " " + splitBySpace[1]).trim();
        }

        int firstEnd = firstSentenceEnd(normalized, 0);
        if (firstEnd < 0) {
            return normalized;
        }
        int secondEnd = firstSentenceEnd(normalized, firstEnd + 1);
        if (secondEnd < 0) {
            return normalized;
        }
        return normalized.substring(0, secondEnd + 1).trim();
    }

    private int firstSentenceEnd(String value, int startIndex) {
        int best = -1;
        for (String marker : List.of(".", "!", "?", "。", "！", "？")) {
            int index = value.indexOf(marker, startIndex);
            if (index >= 0 && (best < 0 || index < best)) {
                best = index;
            }
        }
        return best;
    }

    private String value(Object value) {
        return value == null ? "없음" : String.valueOf(value);
    }
}
