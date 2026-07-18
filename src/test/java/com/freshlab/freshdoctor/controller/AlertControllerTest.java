package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.domain.AlertType;
import com.freshlab.freshdoctor.dto.AlertResponse;
import com.freshlab.freshdoctor.dto.UnreadAlertCountResponse;
import com.freshlab.freshdoctor.security.CurrentUserId;
import com.freshlab.freshdoctor.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AlertControllerTest {

    private AlertService alertService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        alertService = mock(AlertService.class);
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
        mockMvc = MockMvcBuilders.standaloneSetup(new AlertController(alertService))
                .setCustomArgumentResolvers(resolver)
                .build();
    }

    @Test
    void getsAlerts() throws Exception {
        when(alertService.getAlerts(1L)).thenReturn(List.of(response(false)));

        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].alertId").value(10))
                .andExpect(jsonPath("$[0].itemCode").value("1001"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void getsUnreadCount() throws Exception {
        when(alertService.getUnreadCount(1L)).thenReturn(new UnreadAlertCountResponse(4));

        mockMvc.perform(get("/api/alerts/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(4));
    }

    @Test
    void marksAlertAsRead() throws Exception {
        when(alertService.markAsRead(1L, 10L)).thenReturn(response(true));

        mockMvc.perform(patch("/api/alerts/10/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void marksAllAlertsAsRead() throws Exception {
        mockMvc.perform(patch("/api/alerts/read-all"))
                .andExpect(status().isNoContent());

        verify(alertService).markAllAsRead(1L);
    }

    private AlertResponse response(boolean read) {
        return new AlertResponse(
                10L, "1001", "Cabbage", AlertType.GRADE_INCREASE,
                20, "SAFE", 40, "INTEREST", "Title", "Description", "Evidence",
                LocalDate.of(2026, 7, 18), LocalDateTime.of(2026, 7, 18, 10, 0), read
        );
    }
}
