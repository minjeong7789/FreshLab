package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.exception.InvalidPriceDateRangeException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceDateRangeResolverTest {

    private final PriceDateRangeResolver resolver = new PriceDateRangeResolver();

    @Test
    void defaultsToRecentThirtyDaysWhenDatesAreMissing() {
        LocalDate today = LocalDate.now();

        PriceDateRangeResolver.DateRange range = resolver.resolve(null, null);

        assertThat(range.startDate()).isEqualTo(today.minusDays(29));
        assertThat(range.endDate()).isEqualTo(today);
    }

    @Test
    void usesTodayWhenOnlyStartDateExists() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(10);

        PriceDateRangeResolver.DateRange range = resolver.resolve(startDate, null);

        assertThat(range.startDate()).isEqualTo(startDate);
        assertThat(range.endDate()).isEqualTo(today);
    }

    @Test
    void usesRecentThirtyDaysEndingAtGivenEndDate() {
        LocalDate endDate = LocalDate.now().minusDays(10);

        PriceDateRangeResolver.DateRange range = resolver.resolve(null, endDate);

        assertThat(range.startDate()).isEqualTo(endDate.minusDays(29));
        assertThat(range.endDate()).isEqualTo(endDate);
    }

    @Test
    void rejectsInvalidRanges() {
        LocalDate today = LocalDate.now();

        assertThatThrownBy(() -> resolver.resolve(today, today.minusDays(1)))
                .isInstanceOf(InvalidPriceDateRangeException.class);
        assertThatThrownBy(() -> resolver.resolve(today.minusDays(365), today))
                .isInstanceOf(InvalidPriceDateRangeException.class);
        assertThatThrownBy(() -> resolver.resolve(today.plusDays(1), null))
                .isInstanceOf(InvalidPriceDateRangeException.class);
        assertThatThrownBy(() -> resolver.resolve(null, today.plusDays(1)))
                .isInstanceOf(InvalidPriceDateRangeException.class);
    }
}
