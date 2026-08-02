package com.freshlab.freshdoctor.exception;

public class DuplicateWatchItemException extends RuntimeException {

    public DuplicateWatchItemException() {
        super("이미 등록된 관심 품목입니다.");
    }
}
