package com.freshlab.freshdoctor.exception;

public class WatchItemNotFoundException extends RuntimeException {

    public WatchItemNotFoundException() {
        super("등록된 관심 품목을 찾을 수 없습니다.");
    }
}
