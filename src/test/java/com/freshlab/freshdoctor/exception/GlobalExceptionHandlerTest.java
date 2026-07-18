package com.freshlab.freshdoctor.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ErrorTestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void addsTimestampAndPathToDomainErrors() throws Exception {
        mockMvc.perform(get("/test/errors/item"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ITEM_NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").value(containsString("+09:00")))
                .andExpect(jsonPath("$.path").value("/test/errors/item"));
    }

    @Test
    void distinguishesInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/test/errors/date").param("date", "2026/07/28"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"))
                .andExpect(jsonPath("$.path").value("/test/errors/date"));
    }

    @Test
    void distinguishesExternalApiFailure() throws Exception {
        mockMvc.perform(get("/test/errors/external"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("EXTERNAL_API_ERROR"));
    }

    @Test
    void distinguishesInsufficientCalculationData() throws Exception {
        mockMvc.perform(get("/test/errors/insufficient"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_DATA"));
    }

    @Test
    void hidesInternalExceptionDetails() throws Exception {
        mockMvc.perform(get("/test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected server error occurred."))
                .andExpect(content().string(not(containsString("database-password"))));
    }

    @RestController
    @RequestMapping("/test/errors")
    static class ErrorTestController {
        @GetMapping("/item")
        void item() {
            throw new ItemNotFoundException("9999");
        }

        @GetMapping("/date")
        LocalDate date(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
            return date;
        }

        @GetMapping("/external")
        void external() {
            throw new ExternalApiException("provider secret response");
        }

        @GetMapping("/insufficient")
        void insufficient() {
            throw new InsufficientCalculationDataException("Price data is insufficient.");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("database-password=secret");
        }
    }
}
