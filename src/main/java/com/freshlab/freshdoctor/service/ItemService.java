package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.dto.ItemResponse;
import com.freshlab.freshdoctor.exception.ItemNotFoundException;
import com.freshlab.freshdoctor.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public List<ItemResponse> getItems() {
        return itemRepository.findByActiveTrueOrderByItemNameAsc().stream().map(ItemResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> searchItems(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getItems();
        }
        return itemRepository
                .findByActiveTrueAndItemNameContainingIgnoreCaseOrderByItemNameAsc(keyword.trim())
                .stream()
                .map(ItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ItemResponse getItemResponse(String itemCode) {
        return ItemResponse.from(getItem(itemCode));
    }

    @Transactional(readOnly = true)
    public Item getItem(String itemCode) {
        return itemRepository.findById(itemCode).orElseThrow(() -> new ItemNotFoundException(itemCode));
    }
}
