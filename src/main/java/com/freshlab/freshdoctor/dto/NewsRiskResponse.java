package com.freshlab.freshdoctor.dto;

import java.time.LocalDate;
import java.util.List;

public record NewsRiskResponse(
        ComparisonStatus status,
        String itemCode,
        LocalDate baseDate,
        NewsRiskType riskType,
        Integer score,
        String reason,
        List<String> matchedKeywords,
        Long representativeArticleId,
        String representativeArticleTitle,
        String representativeArticleLink,
        String unavailableReason
) {
}
