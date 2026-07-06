package com.freshlab.freshdoctor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshlab.freshdoctor.domain.PriceHistory;
import com.freshlab.freshdoctor.dto.KamisPriceCollectResult;
import com.freshlab.freshdoctor.dto.PriceResponse;
import com.freshlab.freshdoctor.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class KamisPriceService {

    private static final String SOURCE = "KAMIS";
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter KAMIS_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final PriceHistoryRepository priceHistoryRepository;

    @Value("${kamis.api.base-url:https://www.kamis.or.kr/service/price/xml.do}")
    private String baseUrl;

    @Value("${kamis.api.cert-key}")
    private String certKey;

    @Value("${kamis.api.cert-id}")
    private String certId;

    @Transactional
    public KamisPriceCollectResult collectDailyPrice(String itemCode) {
        return collectDailyPrice(itemCode, null);
    }

    @Transactional
    public KamisPriceCollectResult collectDailyPrice(String itemCode, LocalDate regDate) {
        return collectDailyPrice(itemCode, regDate, null, null, null, null, null, null, null);
    }

    @Transactional
    public KamisPriceCollectResult collectDailyPrice(
            String itemCode,
            LocalDate regDate,
            String productClsCode,
            String itemCategoryCode,
            String kamisItemCode,
            String kindCode,
            String productRankCode,
            String countryCode,
            String convertKgYn
    ) {
        if (isBlank(itemCode)) {
            return new KamisPriceCollectResult(itemCode, 0, 0, "itemCode is required.");
        }

        try {
            KamisRequest request = resolveRequest(
                    itemCode,
                    regDate,
                    productClsCode,
                    itemCategoryCode,
                    kamisItemCode,
                    kindCode,
                    productRankCode,
                    countryCode,
                    convertKgYn
            );
            KamisResponse response = requestDailyPrice(request);
            List<KamisPriceRow> rows = new ArrayList<>();
            collectRows(response.root(), request, rows);

            if (rows.isEmpty()) {
                return new KamisPriceCollectResult(
                        itemCode,
                        0,
                        0,
                        "KAMIS price collection returned no parsable rows. responsePreview=" + response.preview()
                );
            }

            int savedCount = 0;
            for (KamisPriceRow row : rows) {
                saveOrUpdate(row);
                savedCount++;
            }

            return new KamisPriceCollectResult(itemCode, rows.size(), savedCount, "KAMIS price collection completed.");
        } catch (Exception ex) {
            return new KamisPriceCollectResult(itemCode, 0, 0, "KAMIS price collection failed: " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<PriceResponse> getPrices(String itemCode, LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedEndDate = endDate == null ? LocalDate.now() : endDate;
        LocalDate resolvedStartDate = startDate == null ? resolvedEndDate.minusDays(30) : startDate;

        return priceHistoryRepository
                .findByItemCodeAndPriceDateBetweenOrderByPriceDateAsc(itemCode, resolvedStartDate, resolvedEndDate)
                .stream()
                .map(PriceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PriceResponse> getPriceTrend(String itemCode, int days) {
        int resolvedDays = Math.max(days, 1);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(resolvedDays);

        List<PriceHistory> prices = priceHistoryRepository
                .findByItemCodeAndPriceDateBetweenOrderByPriceDateAsc(itemCode, startDate, endDate);

        if (prices.isEmpty()) {
            prices = priceHistoryRepository.findTop60ByItemCodeOrderByPriceDateDesc(itemCode)
                    .stream()
                    .sorted(Comparator.comparing(PriceHistory::getPriceDate))
                    .toList();
        }

        return prices.stream()
                .map(PriceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PriceResponse> getPricesByItemName(String itemName, LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedEndDate = endDate == null ? LocalDate.now() : endDate;
        LocalDate resolvedStartDate = startDate == null ? resolvedEndDate.minusDays(30) : startDate;

        return priceHistoryRepository
                .findByItemNameContainingAndPriceDateBetweenOrderByPriceDateAsc(
                        itemName,
                        resolvedStartDate,
                        resolvedEndDate
                )
                .stream()
                .map(PriceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PriceResponse> getPriceTrendByItemName(String itemName, int days) {
        int resolvedDays = Math.max(days, 1);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(resolvedDays);

        List<PriceHistory> prices = priceHistoryRepository
                .findByItemNameContainingAndPriceDateBetweenOrderByPriceDateAsc(itemName, startDate, endDate);

        if (prices.isEmpty()) {
            prices = priceHistoryRepository.findTop60ByItemNameContainingOrderByPriceDateDesc(itemName)
                    .stream()
                    .sorted(Comparator.comparing(PriceHistory::getPriceDate))
                    .toList();
        }

        return prices.stream()
                .map(PriceResponse::from)
                .toList();
    }

    private KamisResponse requestDailyPrice(KamisRequest request) throws Exception {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("action", "dailyPriceByCategoryList")
                .queryParam("p_cert_key", certKey)
                .queryParam("p_cert_id", certId)
                .queryParam("p_returntype", "json")
                .queryParam("p_product_cls_code", request.productClsCode())
                .queryParam("p_regday", request.regDate().format(KAMIS_DATE))
                .queryParam("p_item_category_code", request.itemCategoryCode())
                .queryParam("p_country_code", request.countryCode())
                .queryParam("p_convert_kg_yn", request.convertKgYn());

        addQueryParamIfPresent(uriBuilder, "p_item_code", request.kamisItemCode());
        addQueryParamIfPresent(uriBuilder, "p_kind_code", request.kindCode());
        addQueryParamIfPresent(uriBuilder, "p_product_rank_code", request.productRankCode());

        URI uri = uriBuilder.build(true).toUri();
        String body = webClientBuilder.build()
                .get()
                .uri(uri)
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(responseBody -> {
                            if (response.statusCode().isError()) {
                                throw new IllegalStateException(
                                        "KAMIS HTTP " + response.statusCode().value()
                                                + " responsePreview=" + preview(responseBody)
                                );
                            }
                            return responseBody;
                        }))
                .block();

        if (isBlank(body)) {
            throw new IllegalStateException("Empty KAMIS response.");
        }

        String trimmed = body.trim();
        if (trimmed.startsWith("<")) {
            throw new IllegalStateException("KAMIS returned XML/HTML instead of JSON. responsePreview=" + preview(body));
        }

        return new KamisResponse(objectMapper.readTree(body), preview(body));
    }

    private void saveOrUpdate(KamisPriceRow row) {
        PriceHistory priceHistory = priceHistoryRepository
                .findByItemNameAndPriceDateAndUnitAndSource(
                        row.itemName(),
                        row.priceDate(),
                        row.unit(),
                        SOURCE
                )
                .orElseGet(PriceHistory::new);

        priceHistory.setItemCode(row.itemCode());
        priceHistory.setItemName(row.itemName());
        priceHistory.setKamisItemCode(row.kamisItemCode());
        priceHistory.setKamisKindCode(row.kamisKindCode());
        priceHistory.setKamisRankCode(row.kamisRankCode());
        priceHistory.setPriceDate(row.priceDate());
        priceHistory.setPrice(row.price());
        priceHistory.setUnit(row.unit());
        priceHistory.setMarketType(row.marketType());
        priceHistory.setSource(SOURCE);

        priceHistoryRepository.save(priceHistory);
    }

    private void collectRows(JsonNode node, KamisRequest request, List<KamisPriceRow> rows) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isTextual()) {
            JsonNode parsedTextNode = parseJsonTextNode(node.asText());
            if (parsedTextNode != null) {
                collectRows(parsedTextNode, request, rows);
            }
            return;
        }

        if (node.isObject()) {
            KamisPriceRow row = toRow(node, request);
            if (row != null) {
                rows.add(row);
            }

            Iterator<JsonNode> children = node.elements();
            while (children.hasNext()) {
                collectRows(children.next(), request, rows);
            }
            return;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                collectRows(child, request, rows);
            }
        }
    }

    private KamisPriceRow toRow(JsonNode node, KamisRequest request) {
        PriceValue priceValue = firstPriceValue(node, request);
        LocalDate priceDate = priceValue.priceDate();
        Integer price = priceValue.price();

        if (priceDate == null) {
            priceDate = request.regDate();
        }
        if (price == null) {
            return null;
        }

        String itemName = firstText(node, "item_name", "itemname", "itemName", "productName", "product_name");
        if (isBlank(itemName)) {
            return null;
        }
        String unit = firstText(node, "unit", "unit_name", "unitName");
        String marketType = firstText(node, "market_type", "marketType", "product_cls_name", "productclscode", "countyname");
        String kamisItemCode = firstText(node, "item_code", "itemcode", "itemCode");
        String kamisKindCode = firstText(node, "kind_code", "kindcode", "kindCode");
        String kamisRankCode = firstText(node, "rank_code", "rankcode", "rankCode");

        return new KamisPriceRow(
                isBlank(kamisItemCode) ? request.internalItemCode() : "KAMIS-" + kamisItemCode,
                itemName,
                kamisItemCode,
                kamisKindCode,
                kamisRankCode,
                priceDate,
                price,
                unit,
                isBlank(marketType) ? "UNKNOWN" : marketType
        );
    }

    private PriceValue firstPriceValue(JsonNode node, KamisRequest request) {
        String[][] dayPricePairs = {
                {"day1", "dpr1"},
                {"day2", "dpr2"},
                {"day3", "dpr3"},
                {"day4", "dpr4"}
        };

        for (String[] pair : dayPricePairs) {
            Integer price = parsePrice(firstText(node, pair[1]));
            if (price != null) {
                LocalDate date = parseKamisDayLabel(firstText(node, pair[0]), request.regDate());
                return new PriceValue(date, price);
            }
        }

        LocalDate date = parseDate(firstText(node, "regday", "yyyy", "price_date", "date", "regDate"));
        Integer price = parsePrice(firstText(node, "price", "avg_price", "avgPrice", "value"));
        return new PriceValue(date, price);
    }

    private JsonNode parseJsonTextNode(String value) {
        if (isBlank(value)) {
            return null;
        }

        String trimmed = value.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return null;
        }

        try {
            return objectMapper.readTree(trimmed);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private LocalDate parseDate(String value) {
        if (isBlank(value)) {
            return null;
        }

        String normalized = value.trim().replace(".", "-").replace("/", "-");
        try {
            if (normalized.matches("\\d{8}")) {
                return LocalDate.parse(normalized, BASIC_DATE);
            }
            if (normalized.matches("\\d{1,2}-\\d{1,2}")) {
                String[] parts = normalized.split("-");
                return LocalDate.of(LocalDate.now().getYear(), Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            }
            if (normalized.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                String[] parts = normalized.split("-");
                return LocalDate.of(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2])
                );
            }
            return LocalDate.parse(normalized);
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDate parseKamisDayLabel(String value, LocalDate fallbackDate) {
        if (isBlank(value)) {
            return fallbackDate;
        }

        int open = value.indexOf('(');
        int close = value.indexOf(')');
        if (open >= 0 && close > open) {
            LocalDate parsedDate = parseDate(value.substring(open + 1, close));
            if (parsedDate != null) {
                return parsedDate;
            }
        }

        LocalDate parsedDate = parseDate(value);
        return parsedDate == null ? fallbackDate : parsedDate;
    }

    private Integer parsePrice(String value) {
        if (isBlank(value) || "-".equals(value.trim())) {
            return null;
        }

        String number = value.toLowerCase(Locale.ROOT).replaceAll("[^0-9]", "");
        if (number.isBlank()) {
            return null;
        }
        return Integer.parseInt(number);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void addQueryParamIfPresent(UriComponentsBuilder uriBuilder, String name, String value) {
        if (!isBlank(value)) {
            uriBuilder.queryParam(name, value);
        }
    }

    private KamisRequest resolveRequest(
            String itemCode,
            LocalDate regDate,
            String productClsCode,
            String itemCategoryCode,
            String kamisItemCode,
            String kindCode,
            String productRankCode,
            String countryCode,
            String convertKgYn
    ) {
        LocalDate resolvedRegDate = regDate == null ? LocalDate.now() : regDate;

        return new KamisRequest(
                itemCode,
                resolvedRegDate,
                defaultIfBlank(productClsCode, "01"),
                defaultIfBlank(itemCategoryCode, defaultCategoryCode(itemCode)),
                defaultIfBlank(kamisItemCode, defaultKamisItemCode(itemCode)),
                kindCode,
                productRankCode,
                defaultIfBlank(countryCode, "1101"),
                defaultIfBlank(convertKgYn, "Y")
        );
    }

    private String defaultCategoryCode(String itemCode) {
        return switch (itemCode) {
            case "1001", "1002", "1101", "1201" -> "200";
            default -> "200";
        };
    }

    private String defaultKamisItemCode(String itemCode) {
        return switch (itemCode) {
            case "1001" -> "211";
            case "1002" -> "231";
            case "1101" -> "241";
            case "1201" -> "245";
            default -> null;
        };
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private String preview(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.substring(0, Math.min(normalized.length(), 500));
    }

    private record KamisResponse(JsonNode root, String preview) {
    }

    private record KamisRequest(
            String internalItemCode,
            LocalDate regDate,
            String productClsCode,
            String itemCategoryCode,
            String kamisItemCode,
            String kindCode,
            String productRankCode,
            String countryCode,
            String convertKgYn
    ) {
    }

    private record PriceValue(LocalDate priceDate, Integer price) {
    }

    private record KamisPriceRow(
            String itemCode,
            String itemName,
            String kamisItemCode,
            String kamisKindCode,
            String kamisRankCode,
            LocalDate priceDate,
            Integer price,
            String unit,
            String marketType
    ) {
    }
}
