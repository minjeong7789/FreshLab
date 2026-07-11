package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.exception.InvalidPriceDateRangeException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class PriceDateRangeResolver {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 365;

    public DateRange resolve(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();

        if (startDate != null && startDate.isAfter(today)) {
            throw new InvalidPriceDateRangeException("startDate는 미래 날짜일 수 없습니다.");
        }
        if (endDate != null && endDate.isAfter(today)) {
            throw new InvalidPriceDateRangeException("endDate는 미래 날짜일 수 없습니다.");
        }

        LocalDate resolvedEndDate = endDate == null ? today : endDate;
        LocalDate resolvedStartDate = startDate == null
                ? resolvedEndDate.minusDays(DEFAULT_DAYS - 1L)
                : startDate;

        if (resolvedStartDate.isAfter(resolvedEndDate)) {
            throw new InvalidPriceDateRangeException("startDate는 endDate보다 늦을 수 없습니다.");
        }

        long inclusiveDays = ChronoUnit.DAYS.between(resolvedStartDate, resolvedEndDate) + 1;
        if (inclusiveDays > MAX_DAYS) {
            throw new InvalidPriceDateRangeException("가격 조회 기간은 최대 365일까지 가능합니다.");
        }

        return new DateRange(resolvedStartDate, resolvedEndDate);
    }

    public record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
