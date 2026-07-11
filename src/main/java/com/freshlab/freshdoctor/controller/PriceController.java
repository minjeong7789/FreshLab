package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.dto.KamisPriceCollectResult;
import com.freshlab.freshdoctor.dto.PriceResponse;
import com.freshlab.freshdoctor.dto.PriceTrendResponse;
import com.freshlab.freshdoctor.service.KamisPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PriceController {

    private final KamisPriceService kamisPriceService;

    @PostMapping("/collect/{itemCode}")
    public KamisPriceCollectResult collectPrice(
            @PathVariable String itemCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false) String productClsCode,
            @RequestParam(required = false) String itemCategoryCode,
            @RequestParam(required = false) String kamisItemCode,
            @RequestParam(required = false) String kindCode,
            @RequestParam(required = false) String productRankCode,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String convertKgYn
    ) {
        return kamisPriceService.collectDailyPrice(
                itemCode,
                date,
                productClsCode,
                itemCategoryCode,
                kamisItemCode,
                kindCode,
                productRankCode,
                countryCode,
                convertKgYn
        );
    }

    @GetMapping("/{itemCode}")
    public List<PriceResponse> getPrices(
            @PathVariable String itemCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return kamisPriceService.getPrices(itemCode, startDate, endDate);
    }

    @GetMapping
    public List<PriceResponse> getPricesByItemName(
            @RequestParam String itemName,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return kamisPriceService.getPricesByItemName(itemName, startDate, endDate);
    }

    @GetMapping("/{itemCode}/trend")
    public PriceTrendResponse getPriceTrend(
            @PathVariable String itemCode,
            @RequestParam(defaultValue = "30") int days
    ) {
        return kamisPriceService.getPriceTrend(itemCode, days);
    }

    @GetMapping("/trend")
    public List<PriceResponse> getPriceTrendByItemName(
            @RequestParam String itemName,
            @RequestParam(defaultValue = "30") int days
    ) {
        return kamisPriceService.getPriceTrendByItemName(itemName, days);
    }
}
