package com.booksocial.book.web.dto;

import java.time.Instant;

public record BookResponse(
        String isbn,
        String title,
        String authorName,
        String authorId,
        String description,
        String coverUrl,
        Integer publishedYear,
        String category,
        Instant createdAt) {}
