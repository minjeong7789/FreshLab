package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.NewsArticle;
import com.freshlab.freshdoctor.dto.ComparisonStatus;
import com.freshlab.freshdoctor.dto.NewsRiskResponse;
import com.freshlab.freshdoctor.dto.NewsRiskType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewsRiskCalculatorTest {

    private final NewsRiskCalculator calculator = new NewsRiskCalculator();

    @Test
    void returnsUnavailableWhenArticleListIsEmpty() {
        NewsRiskResponse result = calculator.calculate("1001", List.of());

        assertThat(result.status()).isEqualTo(ComparisonStatus.UNAVAILABLE);
        assertThat(result.score()).isNull();
        assertThat(result.unavailableReason()).isEqualTo("No news articles available.");
    }

    @Test
    void calculatesZeroScoreWhenNoIssueExists() {
        NewsRiskResponse result = calculator.calculate("1001", List.of(
                article(1L, "배추 소비 늘어", "시장 관심이 이어졌다.", "2026-07-19T10:00:00")
        ));

        assertThat(result.riskType()).isEqualTo(NewsRiskType.NONE);
        assertThat(result.score()).isZero();
        assertThat(result.matchedKeywords()).isEmpty();
    }

    @Test
    void calculatesTwoPointScoreFromGoodCropOrSupplyIncrease() {
        NewsRiskResponse result = calculator.calculate("1001", List.of(
                article(1L, "배추 작황 양호", "공급 증가로 가격 안정세", "2026-07-19T10:00:00")
        ));

        assertThat(result.riskType()).isEqualTo(NewsRiskType.GOOD_CROP_OR_SUPPLY_INCREASE);
        assertThat(result.score()).isEqualTo(2);
        assertThat(result.matchedKeywords()).contains("작황 양호", "공급 증가", "안정세");
    }

    @Test
    void calculatesFivePointScoreFromShipmentDecrease() {
        NewsRiskResponse result = calculator.calculate("1001", List.of(
                article(1L, "배추 출하량 감소 예정", "물량 부족 우려", "2026-07-19T10:00:00")
        ));

        assertThat(result.riskType()).isEqualTo(NewsRiskType.SHIPMENT_DECREASE);
        assertThat(result.score()).isEqualTo(5);
        assertThat(result.matchedKeywords()).contains("출하량 감소", "물량 부족");
    }

    @Test
    void calculatesSevenPointScoreFromPestOrHeatDamage() {
        NewsRiskResponse result = calculator.calculate("1001", List.of(
                article(1L, "폭염에 배추 생육 비상", "병해충 피해도 확산", "2026-07-19T10:00:00")
        ));

        assertThat(result.riskType()).isEqualTo(NewsRiskType.PEST_OR_HEAT_DAMAGE);
        assertThat(result.score()).isEqualTo(7);
        assertThat(result.matchedKeywords()).contains("폭염", "병해충");
    }

    @Test
    void calculatesTenPointScoreFromTyphoonOrLargeDamage() {
        NewsRiskResponse result = calculator.calculate("1001", List.of(
                article(1L, "태풍으로 배추 산지 침수", "대규모 피해와 생산 차질", "2026-07-19T10:00:00")
        ));

        assertThat(result.riskType()).isEqualTo(NewsRiskType.TYPHOON_OR_LARGE_DAMAGE);
        assertThat(result.score()).isEqualTo(10);
        assertThat(result.matchedKeywords()).contains("태풍", "대규모 피해", "생산 차질", "침수");
    }

    @Test
    void usesHighestScoreWhenSameDayHasMultipleArticles() {
        NewsRiskResponse result = calculator.calculate("1001", List.of(
                article(1L, "배추 작황 양호", "공급 증가", "2026-07-19T09:00:00"),
                article(2L, "배추 출하량 감소", "수급 불안", "2026-07-19T10:00:00"),
                article(3L, "태풍으로 배추 피해", "산지 침수", "2026-07-19T08:00:00")
        ));

        assertThat(result.score()).isEqualTo(10);
        assertThat(result.representativeArticleId()).isEqualTo(3L);
    }

    @Test
    void usesLatestArticleWhenSameDayScoreIsEqual() {
        NewsRiskResponse result = calculator.calculate("1001", List.of(
                article(1L, "배추 출하량 감소", "수급 불안", "2026-07-19T09:00:00"),
                article(2L, "배추 공급 부족", "물량 부족", "2026-07-19T10:00:00")
        ));

        assertThat(result.score()).isEqualTo(5);
        assertThat(result.representativeArticleId()).isEqualTo(2L);
    }

    @Test
    void rejectsNullArticleList() {
        List<NewsArticle> nullArticles = null;

        assertThatThrownBy(() -> calculator.calculate("1001", nullArticles))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("articles must not be null.");
    }

    private NewsArticle article(Long id, String title, String description, String publishedAt) {
        NewsArticle article = new NewsArticle();
        article.setId(id);
        article.setItemCode("1001");
        article.setQueryText("배추 가격");
        article.setTitle(title);
        article.setDescription(description);
        article.setLink("https://news.example.com/" + id);
        article.setLinkHash("hash-" + id);
        article.setPublishedAt(LocalDateTime.parse(publishedAt));
        article.setCreatedAt(LocalDateTime.parse(publishedAt).plusMinutes(1));
        article.setSource("NAVER");
        return article;
    }
}
