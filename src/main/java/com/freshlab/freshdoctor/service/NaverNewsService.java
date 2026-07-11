package com.freshlab.freshdoctor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.domain.NewsArticle;
import com.freshlab.freshdoctor.dto.NewsCollectResult;
import com.freshlab.freshdoctor.dto.NewsResponse;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NaverNewsService {

    private static final String SOURCE = "NAVER";
    private static final Set<String> TRACKING_QUERY_PARAMETERS = Set.of(
            "fbclid", "gclid", "dclid", "msclkid", "ref", "referrer"
    );
    private static final List<String> RISK_KEYWORDS = List.of(
            "폭염", "한파", "폭우", "태풍", "가뭄", "병해", "수급", "급등", "급락", "부족", "파동", "폐기", "작황", "피해"
    );

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final NewsArticleRepository newsArticleRepository;
    private final ItemService itemService;

    @Value("${naver.news.url:https://openapi.naver.com/v1/search/news.json}")
    private String newsUrl;

    @Value("${naver.news.client-id:${naver.news.Client ID:}}")
    private String clientId;

    @Value("${naver.news.client-secret:${naver.news.Client Secret:}}")
    private String clientSecret;

    @Transactional
    public NewsCollectResult collectNews(String itemCode, String query, int display) {
        Item item = itemService.getItem(itemCode);
        try {
            String resolvedQuery = resolveQuery(item, query);
            int resolvedDisplay = Math.min(Math.max(display, 1), 100);
            JsonNode root = requestNews(resolvedQuery, resolvedDisplay);
            JsonNode items = root.path("items");
            if (!items.isArray()) {
                return new NewsCollectResult(itemCode, resolvedQuery, 0, 0, "Naver news response has no items.");
            }

            int fetchedCount = 0;
            int savedCount = 0;
            for (JsonNode newsItem : items) {
                fetchedCount++;
                if (saveOrUpdate(itemCode, resolvedQuery, newsItem)) {
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
        itemService.getItem(itemCode);
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
        String normalizedLink = normalizeLink(link);
        String linkHash = sha256(normalizedLink);

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

    private String normalizeLink(String link) {
        String trimmed = link.trim();

        try {
            URI uri = new URI(trimmed).normalize();
            String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? null : uri.getHost().toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
                port = -1;
            }

            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                path = "/";
            } else if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            List<String> queryParts = new ArrayList<>();
            String rawQuery = uri.getRawQuery();
            if (!isBlank(rawQuery)) {
                for (String part : rawQuery.split("&")) {
                    String key = part.split("=", 2)[0].toLowerCase(Locale.ROOT);
                    if (!key.startsWith("utm_") && !TRACKING_QUERY_PARAMETERS.contains(key)) {
                        queryParts.add(part);
                    }
                }
            }
            Collections.sort(queryParts);
            String normalizedQuery = queryParts.isEmpty() ? null : String.join("&", queryParts);

            return new URI(
                    scheme,
                    uri.getUserInfo(),
                    host,
                    port,
                    path,
                    normalizedQuery,
                    null
            ).toASCIIString();
        } catch (Exception ignored) {
            return trimmed;
        }
    }

    private String resolveQuery(Item item, String query) {
        if (!isBlank(query)) {
            return query;
        }

        if (!isBlank(item.getNewsKeyword())) {
            return item.getNewsKeyword();
        }
        return item.getItemName() + " 가격";
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
