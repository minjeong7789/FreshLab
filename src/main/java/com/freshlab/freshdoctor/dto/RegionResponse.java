package com.freshlab.freshdoctor.dto;

import com.freshlab.freshdoctor.domain.KamisRegion;

public record RegionResponse(
        String code,
        String name
) {
    public static RegionResponse from(KamisRegion region) {
        return new RegionResponse(region.getCode(), region.getDisplayName());
    }
}
