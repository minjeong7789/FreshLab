package com.freshlab.freshdoctor.support;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

public final class MockMvcTestSupport {

    private MockMvcTestSupport() {
    }

    public static StandaloneMockMvcBuilder standaloneSetup(Object... controllers) {
        MappingJackson2HttpMessageConverter jsonConverter =
                new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .findModulesViaServiceLoader(true)
                                .build()
                );

        return MockMvcBuilders.standaloneSetup(controllers)
                .setMessageConverters(jsonConverter);
    }
}
