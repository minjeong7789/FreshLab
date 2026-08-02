package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.Item;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class NewsArticleFilter {

    private static final Set<String> TRACKING_QUERY_PARAMETERS = Set.of(
            "fbclid", "gclid", "dclid", "msclkid", "ref", "referrer"
    );
    private static final List<String> RISK_KEYWORDS = List.of(
            "가격", "시세", "급등", "급락", "상승", "하락", "인상", "작황", "출하",
            "공급", "수급", "물량", "부족", "감소", "생산량", "병해충", "폭염",
            "호우", "폭우", "태풍", "냉해", "한파", "기상", "피해"
    );
    private static final List<String> AD_KEYWORDS = List.of(
            "광고", "협찬", "이벤트", "쿠폰", "특가", "할인", "맛집", "레시피"
    );
    private static final Set<String> COMMON_QUERY_WORDS = Set.of(
            "가격", "시세", "급등", "급락", "상승", "하락", "뉴스", "기사"
    );
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern SPECIAL_SPACE_PATTERN = Pattern.compile("\\s+");

    public FilterResult filter(Item item, String query, String title, String description, String link) {
        String cleanedTitle = cleanText(title);
        String cleanedDescription = cleanText(description);
        String normalizedLink = normalizeLink(link);
        String combinedText = (cleanedTitle + " " + cleanedDescription).toLowerCase(Locale.ROOT);

        if (isBlank(cleanedTitle) || isBlank(normalizedLink)) {
            return FilterResult.rejected(cleanedTitle, cleanedDescription, normalizedLink);
        }
        if (containsAny(combinedText, AD_KEYWORDS)) {
            return FilterResult.rejected(cleanedTitle, cleanedDescription, normalizedLink);
        }

        List<String> itemTerms = resolveItemTerms(item, query);
        List<String> matchedRiskKeywords = matchedKeywords(combinedText, RISK_KEYWORDS);
        boolean hasItemTerm = containsAny(combinedText, itemTerms);
        boolean relevant = hasItemTerm && !matchedRiskKeywords.isEmpty();

        if (!relevant) {
            return FilterResult.rejected(cleanedTitle, cleanedDescription, normalizedLink);
        }

        int riskScore = Math.min(matchedRiskKeywords.size() * 10, 100);
        return new FilterResult(
                true,
                cleanedTitle,
                cleanedDescription,
                normalizedLink,
                String.join(",", matchedRiskKeywords),
                riskScore
        );
    }

    public String cleanText(String value) {
        if (value == null) {
            return "";
        }
        return SPECIAL_SPACE_PATTERN.matcher(
                        HTML_TAG_PATTERN.matcher(value)
                                .replaceAll("")
                                .replace("&quot;", "\"")
                                .replace("&amp;", "&")
                                .replace("&lt;", "<")
                                .replace("&gt;", ">")
                                .replace("&nbsp;", " ")
                                .replace("&#39;", "'")
                                .replace("&apos;", "'")
                                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ")
                )
                .replaceAll(" ")
                .trim();
    }

    public String normalizeLink(String link) {
        if (isBlank(link)) {
            return "";
        }
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

    private List<String> resolveItemTerms(Item item, String query) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        addTerm(terms, item.getItemName());
        addTerm(terms, item.getNewsKeyword());
        if (!isBlank(query)) {
            for (String token : query.split("\\s+")) {
                if (!COMMON_QUERY_WORDS.contains(token)) {
                    addTerm(terms, token);
                }
            }
        }
        return terms.stream()
                .filter(term -> term.length() >= 2)
                .toList();
    }

    private void addTerm(Set<String> terms, String value) {
        if (isBlank(value)) {
            return;
        }
        for (String token : value.split("\\s+")) {
            String normalized = token.trim();
            if (!normalized.isBlank() && !COMMON_QUERY_WORDS.contains(normalized)) {
                terms.add(normalized.toLowerCase(Locale.ROOT));
            }
        }
    }

    private List<String> matchedKeywords(String text, List<String> keywords) {
        return keywords.stream()
                .filter(keyword -> text.contains(keyword.toLowerCase(Locale.ROOT)))
                .toList();
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream()
                .anyMatch(keyword -> text.contains(keyword.toLowerCase(Locale.ROOT)));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record FilterResult(
            boolean relevant,
            String title,
            String description,
            String normalizedLink,
            String matchedKeywords,
            int riskScore
    ) {

        private static FilterResult rejected(String title, String description, String normalizedLink) {
            return new FilterResult(false, title, description, normalizedLink, "", 0);
        }
    }
}
