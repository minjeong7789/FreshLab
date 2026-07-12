package com.freshlab.freshdoctor.dto;

import com.freshlab.freshdoctor.domain.NewsArticle;

import java.time.LocalDateTime;

public record NewsResponse(
        Long id,
        String itemCode,
        String queryText,
        String title,
        String link,
        String description,
        String matchedKeywords,
        String newsRiskType,
        String riskReason,
        Boolean representativeRisk,
        LocalDateTime publishedAt,
        Integer newsRiskScore,
        String source,
        LocalDateTime createdAt
) {

    public static NewsResponse from(NewsArticle newsArticle) {
        return new NewsResponse(
                newsArticle.getId(),
                newsArticle.getItemCode(),
                newsArticle.getQueryText(),
                newsArticle.getTitle(),
                newsArticle.getLink(),
                newsArticle.getDescription(),
                newsArticle.getMatchedKeywords(),
                newsArticle.getNewsRiskType(),
                newsArticle.getRiskReason(),
                newsArticle.getRepresentativeRisk(),
                newsArticle.getPublishedAt(),
                newsArticle.getNewsRiskScore(),
                newsArticle.getSource(),
                newsArticle.getCreatedAt()
        );
    }
}
