package com.freshlab.freshdoctor.dto;

import com.freshlab.freshdoctor.domain.ActionRecommendation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecommendationResponse(
        Long id,
        String itemCode,
        String itemName,
        String riskGrade,
        Integer finalScore,
        BigDecimal priceIncreaseRate,
        String weatherIssue,
        String newsIssue,
        String recommendation,
        String generationType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static RecommendationResponse from(ActionRecommendation recommendation) {
        return new RecommendationResponse(
                recommendation.getId(),
                recommendation.getItemCode(),
                recommendation.getItemName(),
                recommendation.getRiskGrade(),
                recommendation.getFinalScore(),
                recommendation.getPriceIncreaseRate(),
                recommendation.getWeatherIssue(),
                recommendation.getNewsIssue(),
                recommendation.getRecommendation(),
                recommendation.getGenerationType().name(),
                recommendation.getCreatedAt(),
                recommendation.getUpdatedAt()
        );
    }
}
