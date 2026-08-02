package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.Item;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NewsArticleFilterTest {

    private final NewsArticleFilter filter = new NewsArticleFilter();

    @Test
    void acceptsArticleWhenItemNameAndRiskKeywordExistTogether() {
        NewsArticleFilter.FilterResult result = filter.filter(
                item("배추", "배추 가격"),
                "배추 가격",
                "<b>배추</b> 가격 급등, 산지 출하 감소",
                "폭염 피해로 공급이 줄었다.",
                "https://news.example.com/article?id=1&utm_source=test"
        );

        assertThat(result.relevant()).isTrue();
        assertThat(result.title()).isEqualTo("배추 가격 급등, 산지 출하 감소");
        assertThat(result.normalizedLink()).isEqualTo("https://news.example.com/article?id=1");
        assertThat(result.matchedKeywords()).contains("가격", "급등", "출하", "폭염", "피해", "공급");
        assertThat(result.riskScore()).isGreaterThan(0);
    }

    @Test
    void rejectsArticleWhenItemNameIsMissing() {
        NewsArticleFilter.FilterResult result = filter.filter(
                item("배추", "배추 가격"),
                "배추 가격",
                "농산물 가격 급등",
                "폭염 피해로 출하량이 줄었다.",
                "https://news.example.com/article?id=2"
        );

        assertThat(result.relevant()).isFalse();
    }

    @Test
    void rejectsArticleWhenRiskKeywordIsMissing() {
        NewsArticleFilter.FilterResult result = filter.filter(
                item("배추", "배추 가격"),
                "배추 가격",
                "배추 요리법 소개",
                "오늘 저녁에 먹기 좋은 반찬",
                "https://news.example.com/article?id=3"
        );

        assertThat(result.relevant()).isFalse();
    }

    @Test
    void rejectsAdvertisementArticle() {
        NewsArticleFilter.FilterResult result = filter.filter(
                item("배추", "배추 가격"),
                "배추 가격",
                "배추 가격 특가 이벤트",
                "쿠폰 할인 광고",
                "https://news.example.com/article?id=4"
        );

        assertThat(result.relevant()).isFalse();
    }

    @Test
    void removesHtmlEntitiesAndControlSpaces() {
        String result = filter.cleanText("<b>양파</b>&amp; 가격&nbsp;상승");

        assertThat(result).isEqualTo("양파& 가격 상승");
    }

    private Item item(String itemName, String newsKeyword) {
        Item item = new Item();
        item.setItemCode("1001");
        item.setItemName(itemName);
        item.setNewsKeyword(newsKeyword);
        return item;
    }
}
