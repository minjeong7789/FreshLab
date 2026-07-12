package com.freshlab.freshdoctor.dto;

public enum NewsRiskType {
    NONE(0, "특별한 뉴스·수급 이슈가 없습니다."),
    GOOD_CROP_OR_SUPPLY_INCREASE(2, "작황 양호 또는 공급 증가 신호가 감지되었습니다."),
    SHIPMENT_DECREASE(5, "출하량 감소 또는 수급 불안 신호가 감지되었습니다."),
    PEST_OR_HEAT_DAMAGE(7, "병해충 또는 폭염 피해 신호가 감지되었습니다."),
    TYPHOON_OR_LARGE_DAMAGE(10, "태풍 또는 대규모 작황 피해 신호가 감지되었습니다.");

    private final int score;
    private final String defaultReason;

    NewsRiskType(int score, String defaultReason) {
        this.score = score;
        this.defaultReason = defaultReason;
    }

    public int getScore() {
        return score;
    }

    public String getDefaultReason() {
        return defaultReason;
    }
}
