package com.booksocial.book.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.open-library")
public record OpenLibraryProperties(String apiUrl, String userAgent) {
}
