package com.freshlab.freshdoctor.config;

import com.freshlab.freshdoctor.domain.PriceHistory;
import com.freshlab.freshdoctor.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PriceHistoryDataMigration implements ApplicationRunner {

    private static final String NEW_UNIQUE_KEY = "uk_price_item_date";
    private static final String OLD_UNIQUE_KEY = "uk_price_item_date_market_rank_unit";

    private final PriceHistoryRepository priceHistoryRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        mergeExistingDuplicates();
        ensureItemDateUniqueKey();
    }

    private void mergeExistingDuplicates() {
        Map<ItemDate, List<PriceHistory>> grouped = new LinkedHashMap<>();
        for (PriceHistory history : priceHistoryRepository.findAll()) {
            grouped.computeIfAbsent(
                    new ItemDate(history.getItemCode(), history.getPriceDate()),
                    ignored -> new ArrayList<>()
            ).add(history);
        }

        for (List<PriceHistory> duplicates : grouped.values()) {
            if (duplicates.size() < 2) {
                continue;
            }
            duplicates.sort(Comparator.comparing(PriceHistory::getId));
            PriceHistory representative = duplicates.get(0);
            representative.setPrice(medianPrice(duplicates));
            duplicates.stream()
                    .map(PriceHistory::getNormalYearPrice)
                    .filter(value -> value != null && value > 0)
                    .findFirst()
                    .ifPresent(representative::setNormalYearPrice);
            priceHistoryRepository.save(representative);
            priceHistoryRepository.deleteAll(duplicates.subList(1, duplicates.size()));
        }
        priceHistoryRepository.flush();
    }

    private int medianPrice(List<PriceHistory> rows) {
        List<Integer> prices = rows.stream()
                .map(PriceHistory::getPrice)
                .filter(value -> value != null && value > 0)
                .sorted()
                .toList();
        if (prices.isEmpty()) {
            throw new IllegalStateException("Cannot merge price history rows without a valid price.");
        }
        int middle = prices.size() / 2;
        if (prices.size() % 2 == 1) {
            return prices.get(middle);
        }
        return (int) Math.round((prices.get(middle - 1).longValue() + prices.get(middle).longValue()) / 2.0);
    }

    private void ensureItemDateUniqueKey() {
        if (!indexExists(NEW_UNIQUE_KEY)) {
            jdbcTemplate.execute("ALTER TABLE price_history ADD CONSTRAINT " + NEW_UNIQUE_KEY
                    + " UNIQUE (item_code, price_date)");
        }
        if (indexExists(OLD_UNIQUE_KEY)) {
            jdbcTemplate.execute("ALTER TABLE price_history DROP INDEX " + OLD_UNIQUE_KEY);
        }
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'price_history'
                   AND index_name = ?
                """,
                Integer.class,
                indexName
        );
        return count != null && count > 0;
    }

    private record ItemDate(String itemCode, LocalDate priceDate) {
    }
}
