package com.freshlab.freshdoctor.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PriceValueNormalizer {

    private static final Set<String> MISSING_VALUES = Set.of("-", "없음", "null", "n/a");
    private static final Pattern PRICE_BEFORE_WON = Pattern.compile(
            "^\\s*([0-9]+(?:,[0-9]{3})*)\\s*원(?:\\s*/?.*)?$"
    );
    private static final Pattern PLAIN_PRICE = Pattern.compile(
            "^\\s*([0-9]+(?:,[0-9]{3})*)\\s*$"
    );

    public Integer normalize(String rawPrice) {
        if (rawPrice == null) {
            return null;
        }

        String trimmed = rawPrice.trim();
        if (trimmed.isEmpty() || MISSING_VALUES.contains(trimmed.toLowerCase(Locale.ROOT))) {
            return null;
        }

        Matcher wonMatcher = PRICE_BEFORE_WON.matcher(trimmed);
        if (wonMatcher.matches()) {
            return parseInteger(wonMatcher.group(1));
        }

        Matcher plainMatcher = PLAIN_PRICE.matcher(trimmed);
        if (plainMatcher.matches()) {
            return parseInteger(plainMatcher.group(1));
        }

        return null;
    }

    private Integer parseInteger(String value) {
        try {
            int price = Integer.parseInt(value.replace(",", ""));
            return price > 0 ? price : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
