package com.freshlab.freshdoctor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshlab.freshdoctor.domain.PriceHistory;
import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.dto.CurrentPriceResponse;
import com.freshlab.freshdoctor.dto.KamisPriceCollectResult;
import com.freshlab.freshdoctor.dto.PriceChangeResponse;
import com.freshlab.freshdoctor.dto.PricePointResponse;
import com.freshlab.freshdoctor.dto.PriceResponse;
import com.freshlab.freshdoctor.dto.PriceTrendResponse;
import com.freshlab.freshdoctor.exception.InvalidPricePeriodException;
import com.freshlab.freshdoctor.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KamisPriceService {

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter KAMIS_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ItemService itemService;
    private final PriceValueNormalizer priceValueNormalizer;
    private final PriceDateRangeResolver priceDateRangeResolver;
    private final PriceIncreaseRateCalculator priceIncreaseRateCalculator;

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

        Item item = itemService.getItem(itemCode);
        try {
            KamisRequest request = resolveRequest(
                    item,
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
        Item item = itemService.getItem(itemCode);
        PriceDateRangeResolver.DateRange dateRange = priceDateRangeResolver.resolve(startDate, endDate);

        return priceHistoryRepository
                .findByItemCodeAndMarketTypeAndKamisRankCodeAndUnitAndPriceDateBetweenOrderByPriceDateAsc(
                        itemCode,
                        item.getDefaultMarketType(),
                        item.getDefaultRankCode(),
                        item.getDefaultUnit(),
                        dateRange.startDate(),
                        dateRange.endDate()
                )
                .stream()
                .map(PriceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PriceTrendResponse getPriceTrend(String itemCode, int days) {
        if (days != 7 && days != 14 && days != 30) {
            throw new InvalidPricePeriodException();
        }

        Item item = itemService.getItem(itemCode);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);

        List<PriceHistory> prices = priceHistoryRepository
                .findByItemCodeAndMarketTypeAndKamisRankCodeAndUnitAndPriceDateBetweenOrderByPriceDateAsc(
                        itemCode,
                        item.getDefaultMarketType(),
                        item.getDefaultRankCode(),
                        item.getDefaultUnit(),
                        startDate,
                        endDate
                );

        Map<LocalDate, PriceHistory> pricesByDate = new LinkedHashMap<>();
        for (PriceHistory price : prices) {
            pricesByDate.put(price.getPriceDate(), price);
        }

        PriceHistory seedPrice = priceHistoryRepository
                .findTopByItemCodeAndMarketTypeAndKamisRankCodeAndUnitAndPriceDateLessThanEqualOrderByPriceDateDesc(
                        itemCode,
                        item.getDefaultMarketType(),
                        item.getDefaultRankCode(),
                        item.getDefaultUnit(),
                        startDate
                )
                .orElse(null);
        PriceHistory lastActualPrice = seedPrice;

        List<PricePointResponse> trend = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            PriceHistory actualPrice = pricesByDate.get(date);
            if (actualPrice != null) {
                lastActualPrice = actualPrice;
                trend.add(toTrendPoint(date, actualPrice, false));
            } else if (lastActualPrice != null) {
                trend.add(toTrendPoint(date, lastActualPrice, true));
            }
        }
        PriceHistory latestActualPrice = lastActualPrice;

        CurrentPriceResponse current = null;
        if (!trend.isEmpty()) {
            PricePointResponse latestPoint = trend.get(trend.size() - 1);
            current = new CurrentPriceResponse(
                    latestPoint.price(),
                    latestActualPrice.getUnit(),
                    endDate,
                    latestPoint.actualPriceDate(),
                    latestPoint.carriedForward()
            );
        }

        List<PriceHistory> relevantActualPrices = new ArrayList<>(prices);
        if (seedPrice != null) {
            relevantActualPrices.add(seedPrice);
        }

        Integer normalPrice = relevantActualPrices.stream()
                .filter(price -> price.getNormalYearPrice() != null)
                .max(Comparator.comparing(PriceHistory::getPriceDate))
                .map(PriceHistory::getNormalYearPrice)
                .orElse(null);

        PriceChangeResponse priceChange = getRecentSevenPriceChange(item);

        LocalDateTime lastUpdatedAt = relevantActualPrices.stream()
                .map(PriceHistory::getUpdatedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new PriceTrendResponse(
                item.getItemCode(),
                item.getItemName(),
                current,
                normalPrice,
                priceChange,
                lastUpdatedAt,
                trend
        );
    }

    private PriceChangeResponse getRecentSevenPriceChange(Item item) {
        List<PriceHistory> recentPrices = priceHistoryRepository
                .findTop7ByItemCodeAndMarketTypeAndKamisRankCodeAndUnitOrderByPriceDateDesc(
                        item.getItemCode(),
                        item.getDefaultMarketType(),
                        item.getDefaultRankCode(),
                        item.getDefaultUnit()
                );

        if (recentPrices.size() < 7) {
            return null;
        }

        PriceHistory latest = recentPrices.get(0);
        PriceHistory previous = recentPrices.get(recentPrices.size() - 1);
        return priceIncreaseRateCalculator.calculate(
                previous.getPrice(),
                previous.getPriceDate(),
                latest.getPrice(),
                latest.getPriceDate()
        );
    }

    private PricePointResponse toTrendPoint(
            LocalDate displayDate,
            PriceHistory actualPrice,
            boolean carriedForward
    ) {
        return new PricePointResponse(
                displayDate,
                actualPrice.getPrice(),
                actualPrice.getPriceDate(),
                carriedForward
        );
    }

    @Transactional(readOnly = true)
    public List<PriceResponse> getPricesByItemName(String itemName, LocalDate startDate, LocalDate endDate) {
        PriceDateRangeResolver.DateRange dateRange = priceDateRangeResolver.resolve(startDate, endDate);

        return priceHistoryRepository
                .findByItemNameContainingAndPriceDateBetweenOrderByPriceDateAsc(
                        itemName,
                        dateRange.startDate(),
                        dateRange.endDate()
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
                .queryParam("action", "periodProductList")
                .queryParam("p_cert_key", certKey)
                .queryParam("p_cert_id", certId)
                .queryParam("p_returntype", "json")
                .queryParam("p_productclscode", request.productClsCode())
                .queryParam("p_startday", request.regDate().format(KAMIS_DATE))
                .queryParam("p_endday", request.regDate().format(KAMIS_DATE))
                .queryParam("p_itemcategorycode", request.itemCategoryCode())
                .queryParam("p_countrycode", request.countryCode())
                .queryParam("p_convert_kg_yn", request.convertKgYn());

        addQueryParamIfPresent(uriBuilder, "p_itemcode", request.kamisItemCode());
        addQueryParamIfPresent(uriBuilder, "p_kindcode", request.kindCode());
        addQueryParamIfPresent(uriBuilder, "p_productrankcode", request.productRankCode());

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
        String unit = defaultIfBlank(row.unit(), "UNKNOWN");
        String marketType = defaultIfBlank(row.marketType(), "UNKNOWN");
        String rankCode = defaultIfBlank(row.kamisRankCode(), "UNKNOWN");

        priceHistoryRepository.upsert(
                row.itemCode(),
                row.itemName(),
                row.kamisItemCode(),
                row.kamisKindCode(),
                rankCode,
                row.priceDate(),
                row.price(),
                row.normalYearPrice(),
                unit,
                marketType,
                LocalDateTime.now()
        );
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
        Integer normalYearPrice = priceValueNormalizer.normalize(firstText(node, "dpr7"));

        if (priceDate == null) {
            priceDate = request.regDate();
        }
        if (price == null) {
            return null;
        }

        String responseItemName = firstText(
                node,
                "item_name",
                "itemname",
                "itemName",
                "productName",
                "product_name"
        );
        if (isBlank(responseItemName)) {
            return null;
        }
        String itemName = defaultIfBlank(request.internalItemName(), responseItemName);
        String unit = firstText(node, "unit", "unit_name", "unitName");
        String marketType = firstText(node, "market_type", "marketType", "product_cls_name", "productclscode", "countyname");
        String kamisItemCode = defaultIfBlank(
                firstText(node, "item_code", "itemcode", "itemCode"),
                request.kamisItemCode()
        );
        String kamisKindCode = defaultIfBlank(
                firstText(node, "kind_code", "kindcode", "kindCode"),
                request.kindCode()
        );
        String kamisRankCode = defaultIfBlank(
                firstText(node, "rank_code", "rankcode", "rankCode"),
                request.productRankCode()
        );

        return new KamisPriceRow(
                request.internalItemCode(),
                itemName,
                kamisItemCode,
                kamisKindCode,
                kamisRankCode,
                priceDate,
                price,
                normalYearPrice,
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
            Integer price = priceValueNormalizer.normalize(firstText(node, pair[1]));
            if (price != null) {
                LocalDate date = parseKamisDayLabel(firstText(node, pair[0]), request.regDate());
                return new PriceValue(date, price);
            }
        }

        LocalDate date = parseDate(firstText(node, "regday", "yyyy", "price_date", "date", "regDate"));
        Integer price = priceValueNormalizer.normalize(firstText(node, "price", "avg_price", "avgPrice", "value"));
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void addQueryParamIfPresent(UriComponentsBuilder uriBuilder, String name, String value) {
        if (!isBlank(value)) {
            uriBuilder.queryParam(name, value);
        }
    }

    private KamisRequest resolveRequest(
            Item item,
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
                item.getItemCode(),
                item.getItemName(),
                resolvedRegDate,
                defaultIfBlank(productClsCode, "01"),
                defaultIfBlank(itemCategoryCode, item.getKamisCategoryCode()),
                defaultIfBlank(kamisItemCode, item.getKamisItemCode()),
                defaultIfBlank(kindCode, item.getKamisKindCode()),
                defaultIfBlank(productRankCode, item.getDefaultRankCode()),
                defaultIfBlank(countryCode, "1101"),
                defaultIfBlank(convertKgYn, "Y")
        );
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
            String internalItemName,
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
            Integer normalYearPrice,
            String unit,
            String marketType
    ) {
    }
}
