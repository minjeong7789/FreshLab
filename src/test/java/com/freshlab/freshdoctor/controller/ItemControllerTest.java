package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.dto.ItemResponse;
import com.freshlab.freshdoctor.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemControllerTest {

    private ItemService itemService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        itemService = mock(ItemService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ItemController(itemService)).build();
    }

    @Test
    void searchesItemsWithKeywordQueryParameter() throws Exception {
        when(itemService.searchItems("배")).thenReturn(List.of(itemResponse("1001", "배추")));

        mockMvc.perform(get("/api/items").param("keyword", "배"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemCode").value("1001"))
                .andExpect(jsonPath("$[0].itemName").value("배추"));

        verify(itemService).searchItems("배");
    }

    @Test
    void missingKeywordReturnsAllItems() throws Exception {
        when(itemService.searchItems(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(itemService).searchItems(null);
    }

    private ItemResponse itemResponse(String itemCode, String itemName) {
        return new ItemResponse(
                itemCode, itemName, null, null, null, null, null,
                null, null, null, null, null, null, null
        );
    }
}
