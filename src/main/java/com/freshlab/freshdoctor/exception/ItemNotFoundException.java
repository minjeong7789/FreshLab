package com.freshlab.freshdoctor.exception;

public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(String itemCode) {
        super("Item not found: " + itemCode);
    }
}
