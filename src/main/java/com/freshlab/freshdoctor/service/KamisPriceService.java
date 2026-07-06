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
        if (isBlank(itemCode)) {
            return new KamisPriceCollectResult(itemCode, 0, 0, "itemCode is required.");
        }

        try {
            JsonNode response = requestDailyPrice(itemCode, regDate);
            List<KamisPriceRow> rows = new ArrayList<>();
            collectRows(response, itemCode, rows);

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

    private JsonNode requestDailyPrice(String itemCode, LocalDate regDate) throws Exception {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("action", "dailySalesList")
                .queryParam("p_cert_key", certKey)
                .queryParam("p_cert_id", certId)
                .queryParam("p_returntype", "json")
                .queryParam("p_itemcode", itemCode);

        if (regDate != null) {
            uriBuilder.queryParam("p_regday", regDate.format(BASIC_DATE));
        }

        URI uri = uriBuilder.build(true).toUri();
        String body = webClientBuilder.build()
                .get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (isBlank(body)) {
            throw new IllegalStateException("Empty KAMIS response.");
        }
        return objectMapper.readTree(body);
    }

    private void saveOrUpdate(KamisPriceRow row) {
        PriceHistory priceHistory = priceHistoryRepository
                .findByItemCodeAndPriceDateAndMarketTypeAndSource(
                        row.itemCode(),
                        row.priceDate(),
                        row.marketType(),
                        SOURCE
                )
                .orElseGet(PriceHistory::new);

        priceHistory.setItemCode(row.itemCode());
        priceHistory.setItemName(row.itemName());
        priceHistory.setPriceDate(row.priceDate());
        priceHistory.setPrice(row.price());
        priceHistory.setUnit(row.unit());
        priceHistory.setMarketType(row.marketType());
        priceHistory.setSource(SOURCE);

        priceHistoryRepository.save(priceHistory);
    }

    private void collectRows(JsonNode node, String requestedItemCode, List<KamisPriceRow> rows) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            KamisPriceRow row = toRow(node, requestedItemCode);
            if (row != null) {
                rows.add(row);
            }

            Iterator<JsonNode> children = node.elements();
            while (children.hasNext()) {
                collectRows(children.next(), requestedItemCode, rows);
            }
            return;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                collectRows(child, requestedItemCode, rows);
            }
        }
    }

    private KamisPriceRow toRow(JsonNode node, String requestedItemCode) {
        LocalDate priceDate = parseDate(firstText(node, "regday", "yyyy", "price_date", "date", "regDate"));
        Integer price = parsePrice(firstText(node, "price", "dpr1", "avg_price", "avgPrice", "value"));

        if (priceDate == null || price == null) {
            return null;
        }

        String itemCode = firstText(node, "item_code", "itemcode", "itemCode", "productno");
        String itemName = firstText(node, "item_name", "itemname", "itemName", "productName", "product_name");
        String unit = firstText(node, "unit", "unit_name", "unitName");
        String marketType = firstText(node, "market_type", "marketType", "product_cls_name", "productclscode", "countyname");

        return new KamisPriceRow(
                isBlank(itemCode) ? requestedItemCode : itemCode,
                itemName,
                priceDate,
                price,
                unit,
                isBlank(marketType) ? "UNKNOWN" : marketType
        );
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

    private record KamisPriceRow(
            String itemCode,
            String itemName,
            LocalDate priceDate,
            Integer price,
            String unit,
            String marketType
    ) {
    }
}
