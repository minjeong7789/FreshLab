package com.freshlab.freshdoctor.dto;

import com.freshlab.freshdoctor.domain.Item;

public record ItemResponse(
        String itemCode,
        String itemName,
        String kamisCategoryCode,
        String kamisItemCode,
        String kamisKindCode,
        String unit,
        String grade,
        String defaultMarketType,
        String defaultRankCode,
        String defaultUnit,
        String weatherRegion,
        Integer weatherNx,
        Integer weatherNy,
        String newsKeyword
) {
    public static ItemResponse from(Item item) {
        return new ItemResponse(
                item.getItemCode(), item.getItemName(), item.getKamisCategoryCode(),
                item.getKamisItemCode(), item.getKamisKindCode(), item.getUnit(), item.getGrade(),
                item.getDefaultMarketType(), item.getDefaultRankCode(), item.getDefaultUnit(),
                item.getWeatherRegion(), item.getWeatherNx(), item.getWeatherNy(), item.getNewsKeyword()
        );
    }
}
