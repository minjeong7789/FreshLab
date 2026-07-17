package com.freshlab.freshdoctor.exception;

public class InvalidRegionException extends RuntimeException {

    public InvalidRegionException() {
        super("KAMIS에서 지원하는 지역을 선택해야 합니다.");
    }
}
