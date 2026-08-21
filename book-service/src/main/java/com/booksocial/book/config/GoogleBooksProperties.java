package com.booksocial.book.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.google-books")
public record GoogleBooksProperties(String apiKey, String apiUrl) {}
