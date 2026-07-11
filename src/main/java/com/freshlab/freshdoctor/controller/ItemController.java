package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.dto.ItemResponse;
import com.freshlab.freshdoctor.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @GetMapping
    public List<ItemResponse> getItems() {
        return itemService.getItems();
    }

    @GetMapping("/{itemCode}")
    public ItemResponse getItem(@PathVariable String itemCode) {
        return itemService.getItemResponse(itemCode);
    }
}
