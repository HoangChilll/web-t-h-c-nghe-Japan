package com.nihongolisten.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.crawler")
public record CrawlerProperties(
        String baseUrl,
        String internalSecret
) {
}
