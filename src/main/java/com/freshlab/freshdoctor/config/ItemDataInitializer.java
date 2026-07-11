package com.freshlab.freshdoctor.config;

import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ItemDataInitializer implements CommandLineRunner {
    private final ItemRepository itemRepository;

    @Override
    public void run(String... args) {
        List<Seed> seeds = List.of(
                new Seed("1001", "배추", "200", "211", null, "1포기", "상품", "광주", 58, 74, "배추 가격 수급"),
                new Seed("1002", "무", "200", "231", null, "1개", "상품", "광주", 58, 74, "무 가격 수급"),
                new Seed("1003", "양파", "200", "245", null, "1kg", "상품", "광주", 58, 74, "양파 가격 수급"),
                new Seed("1004", "감자", "100", "152", null, "1kg", "상품", "광주", 58, 74, "감자 가격 수급"),
                new Seed("1005", "대파", "200", "246", null, "1kg", "상품", "광주", 58, 74, "대파 가격 수급")
        );
        for (Seed seed : seeds) {
            Item item = itemRepository.findById(seed.itemCode()).orElseGet(Item::new);
            seed.apply(item);
            itemRepository.save(item);
        }
    }

    private record Seed(String itemCode, String itemName, String categoryCode, String kamisItemCode,
                        String kindCode, String unit, String grade, String region, int nx, int ny,
                        String newsKeyword) {
        void apply(Item item) {
            item.setItemCode(itemCode);
            item.setItemName(itemName);
            item.setKamisCategoryCode(categoryCode);
            item.setKamisItemCode(kamisItemCode);
            item.setKamisKindCode(kindCode);
            item.setUnit(unit);
            item.setGrade(grade);
            item.setDefaultMarketType("소매");
            item.setDefaultRankCode("04");
            item.setDefaultUnit(unit);
            item.setWeatherRegion(region);
            item.setWeatherNx(nx);
            item.setWeatherNy(ny);
            item.setNewsKeyword(newsKeyword);
            item.setItemType(Item.ItemType.DOMESTIC);
            item.setActive(true);
        }
    }
}
