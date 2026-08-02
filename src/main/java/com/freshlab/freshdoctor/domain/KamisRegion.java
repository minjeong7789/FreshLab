package com.freshlab.freshdoctor.domain;

import java.util.Arrays;
import java.util.Optional;

public enum KamisRegion {
    SEOUL("1101", "서울"),
    BUSAN("2100", "부산"),
    DAEGU("2200", "대구"),
    INCHEON("2300", "인천"),
    GWANGJU("2401", "광주"),
    DAEJEON("2501", "대전"),
    ULSAN("2601", "울산"),
    SUWON("3111", "수원"),
    GANGNEUNG("3214", "강릉"),
    CHUNCHEON("3211", "춘천"),
    CHEONGJU("3311", "청주"),
    JEONJU("3511", "전주"),
    POHANG("3711", "포항"),
    JEJU("3911", "제주"),
    UIJEONGBU("3113", "의정부"),
    SUNCHEON("3613", "순천"),
    ANDONG("3714", "안동"),
    CHANGWON("3814", "창원"),
    YONGIN("3145", "용인"),
    SEJONG("2701", "세종"),
    SEONGNAM("3112", "성남"),
    GOYANG("3138", "고양"),
    CHEONAN("3411", "천안"),
    GIMHAE("3818", "김해");

    private final String code;
    private final String displayName;

    KamisRegion(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Optional<KamisRegion> findByDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(region -> region.displayName.equals(displayName))
                .findFirst();
    }
}
