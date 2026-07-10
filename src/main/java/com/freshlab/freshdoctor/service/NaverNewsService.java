package com.freshlab.freshdoctor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.domain.NewsArticle;
import com.freshlab.freshdoctor.dto.NewsCollectResult;
import com.freshlab.freshdoctor.dto.NewsResponse;
import com.freshlab.freshdoctor.repository.ItemRepository;
import com.freshlab.freshdoctor.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NaverNewsService {

    private static final String SOURCE = "NAVER";
    private static final List<String> RISK_KEYWORDS = List.of(
            "폭염", "한파", "폭우", "태풍", "가뭄", "병해", "수급", "급등", "급락", "부족", "파동", "폐기", "작황", "피해"
    );

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final ItemRepository itemRepository;
    private final NewsArticleRepository newsArticleRepository;

    @Value("${naver.news.url:https://openapi.naver.com/v1/search/news.json}")
    private String newsUrl;

    @Value("${naver.news.client-id:${naver.news.Client ID:}}")
    private String clientId;

    @Value("${naver.news.client-secret:${naver.news.Client Secret:}}")
    private String clientSecret;

    @Transactional
    public NewsCollectResult collectNews(String itemCode, String query, int display) {
        try {
            String resolvedQuery = resolveQuery(itemCode, query);
            int resolvedDisplay = Math.min(Math.max(display, 1), 100);
            JsonNode root = requestNews(resolvedQuery, resolvedDisplay);
            JsonNode items = root.path("items");
            if (!items.isArray()) {
                return new NewsCollectResult(itemCode, resolvedQuery, 0, 0, "Naver news response has no items.");
            }

            int fetchedCount = 0;
            int savedCount = 0;
            for (JsonNode item : items) {
                fetchedCount++;
                if (saveOrUpdate(itemCode, resolvedQuery, item)) {
                    savedCount++;
                }
            }

            return new NewsCollectResult(itemCode, resolvedQuery, fetchedCount, savedCount, "Naver news collection completed.");
        } catch (Exception ex) {
            return new NewsCollectResult(itemCode, query, 0, 0, "Naver news collection failed: " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<NewsResponse> getNews(String itemCode) {
        return newsArticleRepository.findTop20ByItemCodeOrderByPublishedAtDescCreatedAtDesc(itemCode)
                .stream()
                .map(NewsResponse::from)
                .toList();
    }

    private JsonNode requestNews(String query, int display) throws Exception {
        if (isBlank(clientId) || isBlank(clientSecret)) {
            throw new IllegalStateException("Naver news API credentials are empty.");
        }

        URI uri = UriComponentsBuilder.fromUriString(newsUrl)
                .queryParam("query", query)
                .queryParam("display", display)
                .queryParam("sort", "date")
                .build()
                .encode()
                .toUri();

        String body = webClientBuilder.build()
                .get()
                .uri(uri)
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (isBlank(body)) {
            throw new IllegalStateException("Empty Naver news response.");
        }
        return objectMapper.readTree(body);
    }

    private boolean saveOrUpdate(String itemCode, String query, JsonNode node) throws Exception {
        String title = cleanHtml(node.path("title").asText(""));
        String link = node.path("originallink").asText(node.path("link").asText(""));
        if (isBlank(link)) {
            link = node.path("link").asText("");
        }

        if (isBlank(title) || isBlank(link)) {
            return false;
        }

        String description = cleanHtml(node.path("description").asText(""));
        String linkHash = sha256(link);

        NewsArticle newsArticle = newsArticleRepository
                .findByItemCodeAndLinkHash(itemCode, linkHash)
                .orElseGet(NewsArticle::new);

        newsArticle.setItemCode(itemCode);
        newsArticle.setQueryText(query);
        newsArticle.setTitle(title);
        newsArticle.setLink(link);
        newsArticle.setLinkHash(linkHash);
        newsArticle.setDescription(description);
        newsArticle.setPublishedAt(parsePublishedAt(node.path("pubDate").asText(null)));
        newsArticle.setNewsRiskScore(calculateRiskScore(title + " " + description));
        newsArticle.setSource(SOURCE);

        newsArticleRepository.save(newsArticle);
        return true;
    }

    private String resolveQuery(String itemCode, String query) {
        if (!isBlank(query)) {
            return query;
        }

        return itemRepository.findById(itemCode)
                .map(Item::getItemName)
                .filter(name -> !name.isBlank())
                .map(name -> name + " 가격")
                .orElse(itemCode + " 가격");
    }

    private Integer calculateRiskScore(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        int matches = 0;
        for (String keyword : RISK_KEYWORDS) {
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                matches++;
            }
        }
        return Math.min(matches * 15, 100);
    }

    private LocalDateTime parsePublishedAt(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String cleanHtml(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("<[^>]*>", "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim();
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
