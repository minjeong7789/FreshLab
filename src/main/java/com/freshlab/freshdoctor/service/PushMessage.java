package com.freshlab.freshdoctor.service;

import java.util.Map;

public record PushMessage(
        String title,
        String body,
        String targetUrl,
        Map<String, String> data
) {
}
