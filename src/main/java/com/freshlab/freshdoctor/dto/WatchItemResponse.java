package com.freshlab.freshdoctor.dto;

import com.freshlab.freshdoctor.domain.UserItem;

public record WatchItemResponse(
        String itemCode,
        String itemName,
        boolean notificationEnabled
) {
    public static WatchItemResponse from(UserItem userItem) {
        return new WatchItemResponse(
                userItem.getItem().getItemCode(),
                userItem.getItem().getItemName(),
                Boolean.TRUE.equals(userItem.getNotificationEnabled())
        );
    }
}
