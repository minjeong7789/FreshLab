package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.NewsArticle;
import com.freshlab.freshdoctor.dto.ComparisonStatus;
import com.freshlab.freshdoctor.dto.NewsRiskResponse;
import com.freshlab.freshdoctor.dto.NewsRiskType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Component
public class NewsRiskCalculator {

    private static final List<String> GOOD_CROP_KEYWORDS = List.of(
            "작황 양호", "공급 증가", "출하 증가", "생산량 증가", "안정세", "가격 안정"
    );
    private static final List<String> SHIPMENT_DECREASE_KEYWORDS = List.of(
            "출하 감소", "출하량 감소", "공급 부족", "물량 부족", "수급 불안", "생산량 감소"
    );
    private static final List<String> PEST_OR_HEAT_KEYWORDS = List.of(
            "병해충", "폭염", "냉해", "호우 피해", "폭우 피해", "기상 피해"
    );
    private static final List<String> TYPHOON_OR_LARGE_DAMAGE_KEYWORDS = List.of(
            "태풍", "대규모 피해", "작황 피해", "생산 차질", "침수"
    );

    public NewsRiskResponse calculate(String itemCode, List<NewsArticle> articles) {
        Objects.requireNonNull(articles, "articles must not be null.");

        if (articles.isEmpty()) {
            return new NewsRiskResponse(
                    ComparisonStatus.UNAVAILABLE,
                    itemCode,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    "No news articles available."
            );
        }

        LocalDate latestDate = articles.stream()
                .map(this::resolveArticleDate)
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        NewsArticleRisk representative = articles.stream()
                .filter(article -> resolveArticleDate(article).equals(latestDate))
                .map(this::calculateArticleRisk)
                .max(Comparator
                        .comparingInt(NewsArticleRisk::score)
                        .thenComparing(NewsArticleRisk::publishedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(NewsArticleRisk::createdAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseGet(() -> calculateArticleRisk(articles.get(0)));

        return new NewsRiskResponse(
                ComparisonStatus.CALCULATED,
                itemCode,
                latestDate,
                representative.riskType(),
                representative.score(),
                representative.reason(),
                representative.matchedKeywords(),
                representative.article().getId(),
                representative.article().getTitle(),
                representative.article().getLink(),
                null
        );
    }

    public NewsArticleRisk calculateArticleRisk(NewsArticle article) {
        String text = normalize(article.getTitle() + " " + article.getDescription());

        NewsRiskType riskType = NewsRiskType.NONE;
        Set<String> matchedKeywords = new LinkedHashSet<>();
        if (collectMatches(text, TYPHOON_OR_LARGE_DAMAGE_KEYWORDS, matchedKeywords)) {
            riskType = NewsRiskType.TYPHOON_OR_LARGE_DAMAGE;
        } else if (collectMatches(text, PEST_OR_HEAT_KEYWORDS, matchedKeywords)) {
            riskType = NewsRiskType.PEST_OR_HEAT_DAMAGE;
        } else if (collectMatches(text, SHIPMENT_DECREASE_KEYWORDS, matchedKeywords)) {
            riskType = NewsRiskType.SHIPMENT_DECREASE;
        } else if (collectMatches(text, GOOD_CROP_KEYWORDS, matchedKeywords)) {
            riskType = NewsRiskType.GOOD_CROP_OR_SUPPLY_INCREASE;
        }

        return new NewsArticleRisk(
                article,
                riskType,
                riskType.getScore(),
                riskType.getDefaultReason(),
                List.copyOf(matchedKeywords),
                article.getPublishedAt(),
                article.getCreatedAt()
        );
    }

    private boolean collectMatches(String text, List<String> keywords, Set<String> matchedKeywords) {
        List<String> matches = keywords.stream()
                .filter(keyword -> text.contains(keyword.toLowerCase(Locale.ROOT)))
                .toList();
        matchedKeywords.addAll(matches);
        return !matches.isEmpty();
    }

    private LocalDate resolveArticleDate(NewsArticle article) {
        if (article.getPublishedAt() != null) {
            return article.getPublishedAt().toLocalDate();
        }
        if (article.getCreatedAt() != null) {
            return article.getCreatedAt().toLocalDate();
        }
        return LocalDate.now();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT);
    }

    public record NewsArticleRisk(
            NewsArticle article,
            NewsRiskType riskType,
            int score,
            String reason,
            List<String> matchedKeywords,
            LocalDateTime publishedAt,
            LocalDateTime createdAt
    ) {
    }
}
