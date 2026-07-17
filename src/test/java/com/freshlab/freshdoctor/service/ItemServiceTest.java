package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.dto.ItemResponse;
import com.freshlab.freshdoctor.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(itemRepository);
    }

    @Test
    void searchesRegisteredActiveItemsByTrimmedKeyword() {
        when(itemRepository.findByActiveTrueAndItemNameContainingIgnoreCaseOrderByItemNameAsc("배"))
                .thenReturn(List.of(item("1001", "배추")));

        List<ItemResponse> result = itemService.searchItems("  배  ");

        assertThat(result).extracting(ItemResponse::itemCode).containsExactly("1001");
        assertThat(result).extracting(ItemResponse::itemName).containsExactly("배추");
    }

    @Test
    void returnsEmptyListWhenNoRegisteredItemMatches() {
        when(itemRepository.findByActiveTrueAndItemNameContainingIgnoreCaseOrderByItemNameAsc("망고"))
                .thenReturn(List.of());

        assertThat(itemService.searchItems("망고")).isEmpty();
    }

    @Test
    void blankKeywordReturnsAllActiveItems() {
        when(itemRepository.findByActiveTrueOrderByItemNameAsc())
                .thenReturn(List.of(item("1004", "감자"), item("1001", "배추")));

        List<ItemResponse> result = itemService.searchItems("   ");

        assertThat(result).extracting(ItemResponse::itemCode).containsExactly("1004", "1001");
        verify(itemRepository, never())
                .findByActiveTrueAndItemNameContainingIgnoreCaseOrderByItemNameAsc("   ");
    }

    private Item item(String itemCode, String itemName) {
        Item item = new Item();
        item.setItemCode(itemCode);
        item.setItemName(itemName);
        item.setActive(true);
        return item;
    }
}
