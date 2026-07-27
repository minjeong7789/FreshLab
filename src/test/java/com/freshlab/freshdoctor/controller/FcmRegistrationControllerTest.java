package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.domain.FcmPlatform;
import com.freshlab.freshdoctor.dto.FcmRegistrationResponse;
import com.freshlab.freshdoctor.security.CurrentUserId;
import com.freshlab.freshdoctor.service.FcmRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FcmRegistrationControllerTest {

    private FcmRegistrationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(FcmRegistrationService.class);
        HandlerMethodArgumentResolver resolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(CurrentUserId.class);
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    org.springframework.web.method.support.ModelAndViewContainer container,
                    org.springframework.web.context.request.NativeWebRequest request,
                    org.springframework.web.bind.support.WebDataBinderFactory factory
            ) {
                return 1L;
            }
        };
        mockMvc = MockMvcBuilders.standaloneSetup(new FcmRegistrationController(service))
                .setCustomArgumentResolvers(resolver)
                .build();
    }

    @Test
    void registersCurrentUsersBrowserWithoutReturningRegistrationKey() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 27, 10, 0);
        when(service.register(eq(1L), any())).thenReturn(
                new FcmRegistrationResponse(10L, FcmPlatform.WEB, "Chrome", true, now, now));

        mockMvc.perform(post("/api/users/me/fcm-registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "registrationKey": "secret-browser-token",
                                  "platform": "WEB",
                                  "deviceName": "Chrome"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        verify(service).register(eq(1L), argThat(request ->
                request.registrationKey().equals("secret-browser-token")
                        && request.platform() == FcmPlatform.WEB
                        && request.deviceName().equals("Chrome")));
    }

    @Test
    void deactivatesOnlyCurrentUsersRegistration() throws Exception {
        mockMvc.perform(delete("/api/users/me/fcm-registrations/10"))
                .andExpect(status().isNoContent());

        verify(service).unregister(1L, 10L);
    }
}
