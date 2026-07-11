package com.freshlab.freshdoctor.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PriceValueNormalizerTest {

    private final PriceValueNormalizer normalizer = new PriceValueNormalizer();

    @Test
    void normalizesCommaAndWonPrices() {
        assertThat(normalizer.normalize("12,500")).isEqualTo(12_500);
        assertThat(normalizer.normalize("12,500원")).isEqualTo(12_500);
        assertThat(normalizer.normalize(" 8,900 ")).isEqualTo(8_900);
    }

    @Test
    void extractsOnlyTheNumberBeforeWon() {
        assertThat(normalizer.normalize("12,500원/20kg")).isEqualTo(12_500);
        assertThat(normalizer.normalize("12,500원 / 20kg")).isEqualTo(12_500);
    }

    @Test
    void returnsNullForMissingOrInvalidPrices() {
        assertThat(normalizer.normalize(null)).isNull();
        assertThat(normalizer.normalize("")).isNull();
        assertThat(normalizer.normalize("-")).isNull();
        assertThat(normalizer.normalize("없음")).isNull();
        assertThat(normalizer.normalize("N/A")).isNull();
        assertThat(normalizer.normalize("가격 12,500원")).isNull();
        assertThat(normalizer.normalize("12,34원")).isNull();
        assertThat(normalizer.normalize("0원")).isNull();
        assertThat(normalizer.normalize("999999999999원")).isNull();
    }
}
