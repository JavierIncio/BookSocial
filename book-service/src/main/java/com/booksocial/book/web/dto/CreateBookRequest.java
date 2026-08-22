package com.booksocial.book.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBookRequest(
        @NotBlank @Size(max = 20) String isbn,
        @NotBlank String title,
        @NotBlank String authorName,
        String authorId,
        String description,
        String coverUrl,
        Integer publishedYear,
        String category) {}
