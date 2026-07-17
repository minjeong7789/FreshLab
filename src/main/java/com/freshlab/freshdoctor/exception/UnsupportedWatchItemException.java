package com.freshlab.freshdoctor.exception;

public class UnsupportedWatchItemException extends RuntimeException {

    public UnsupportedWatchItemException() {
        super("MVP에서 선택할 수 없는 품목입니다.");
    }
}
