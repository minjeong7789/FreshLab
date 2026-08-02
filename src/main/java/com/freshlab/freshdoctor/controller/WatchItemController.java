package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.dto.WatchItemResponse;
import com.freshlab.freshdoctor.security.CurrentUserId;
import com.freshlab.freshdoctor.service.WatchItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/items")
@RequiredArgsConstructor
public class WatchItemController {

    private final WatchItemService watchItemService;

    @GetMapping
    public List<WatchItemResponse> getWatchItems(@CurrentUserId Long userId) {
        return watchItemService.getWatchItems(userId);
    }

    @PostMapping("/{itemCode}")
    @ResponseStatus(HttpStatus.CREATED)
    public WatchItemResponse addWatchItem(
            @CurrentUserId Long userId,
            @PathVariable String itemCode
    ) {
        return watchItemService.addWatchItem(userId, itemCode);
    }

    @DeleteMapping("/{itemCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWatchItem(
            @CurrentUserId Long userId,
            @PathVariable String itemCode
    ) {
        watchItemService.deleteWatchItem(userId, itemCode);
    }

}
