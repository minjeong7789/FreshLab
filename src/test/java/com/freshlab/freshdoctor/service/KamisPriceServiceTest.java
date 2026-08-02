package com.freshlab.freshdoctor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KamisPriceServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KamisPriceService service = new KamisPriceService(
            null,
            objectMapper,
            null,
            null,
            new PriceValueNormalizer(),
            null,
            null
    );

    @Test
    void parsesActualDailyCategoryJsonFieldNamesAndMatchesRank() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "data": {
                    "item": [
                      {"item_code":"211","kind_code":"01","rank_code":"05","dpr7":"3,100"},
                      {"item_code":"211","kind_code":"01","rank_code":"04","dpr7":"4,473"}
                    ]
                  }
                }
                """);

        Integer result = service.findNormalYearPrice(response, "211", null, "04");

        assertThat(result).isEqualTo(4_473);
    }

    @Test
    void alsoSupportsDocumentedFieldNames() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {"data":{"item":[{"itemcode":"211","kindcode":"01","rankcode":"04","dpr7":"4,473"}]}}
                """);

        Integer result = service.findNormalYearPrice(response, "211", "01", "04");

        assertThat(result).isEqualTo(4_473);
    }

    @Test
    void doesNotUseAnotherItemOrRankNormalYearPrice() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {"data":{"item":[
                  {"item_code":"211","rank_code":"05","dpr7":"3,100"},
                  {"item_code":"212","rank_code":"04","dpr7":"3,710"}
                ]}}
                """);

        Integer result = service.findNormalYearPrice(response, "211", null, "04");

        assertThat(result).isNull();
    }

    @Test
    void convertsPotatoNormalYearPriceFromOneHundredGramsToOneKilogram() {
        Integer result = service.convertPriceUnit(344, "100g", "1kg");

        assertThat(result).isEqualTo(3_440);
    }

    @Test
    void keepsPriceWhenUnitsAreAlreadyEqual() {
        assertThat(service.convertPriceUnit(4_473, "1포기", "1포기")).isEqualTo(4_473);
    }

    @Test
    void rejectsIncompatibleUnitsInsteadOfComparingDifferentUnits() {
        assertThat(service.convertPriceUnit(344, "100g", "1개")).isNull();
    }
}
