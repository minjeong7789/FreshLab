package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.dto.WatchItemResponse;
import com.freshlab.freshdoctor.security.CurrentUserId;
import com.freshlab.freshdoctor.service.WatchItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WatchItemControllerTest {

    private WatchItemService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(WatchItemService.class);
        HandlerMethodArgumentResolver resolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(CurrentUserId.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          org.springframework.web.method.support.ModelAndViewContainer container,
                                          org.springframework.web.context.request.NativeWebRequest request,
                                          org.springframework.web.bind.support.WebDataBinderFactory factory) {
                return 1L;
            }
        };
        mockMvc = MockMvcBuilders.standaloneSetup(new WatchItemController(service))
                .setCustomArgumentResolvers(resolver)
                .build();
    }

    @Test
    void getsWatchItems() throws Exception {
        when(service.getWatchItems(1L)).thenReturn(List.of(new WatchItemResponse("1001", "배추", true)));

        mockMvc.perform(get("/api/users/me/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemCode").value("1001"))
                .andExpect(jsonPath("$[0].notificationEnabled").value(true));
    }

    @Test
    void addsWatchItem() throws Exception {
        when(service.addWatchItem(1L, "1001"))
                .thenReturn(new WatchItemResponse("1001", "배추", true));

        mockMvc.perform(post("/api/users/me/items/1001"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemCode").value("1001"));
    }

    @Test
    void deletesWatchItem() throws Exception {
        mockMvc.perform(delete("/api/users/me/items/1001"))
                .andExpect(status().isNoContent());

        verify(service).deleteWatchItem(1L, "1001");
    }

    @Test
    void updatesNotification() throws Exception {
        when(service.updateNotification(1L, "1001", false))
                .thenReturn(new WatchItemResponse("1001", "배추", false));

        mockMvc.perform(patch("/api/users/me/items/1001/notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationEnabled").value(false));
    }
}
