package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.domain.KamisRegion;
import com.freshlab.freshdoctor.dto.RegionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/regions")
public class RegionController {

    @GetMapping
    public List<RegionResponse> getRegions() {
        return Arrays.stream(KamisRegion.values())
                .map(RegionResponse::from)
                .toList();
    }
}
