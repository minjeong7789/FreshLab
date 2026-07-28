package com.freshlab.freshdoctor.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static com.freshlab.freshdoctor.support.MockMvcTestSupport.standaloneSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegionControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new RegionController()).build();
    }

    @Test
    void returnsAllKamisRetailRegions() throws Exception {
        mockMvc.perform(get("/api/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(24))
                .andExpect(jsonPath("$[0].code").value("1101"))
                .andExpect(jsonPath("$[0].name").value("서울"))
                .andExpect(jsonPath("$[23].code").value("3818"))
                .andExpect(jsonPath("$[23].name").value("김해"));
    }
}
