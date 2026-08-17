package com.booksocial.shelf.web.dto;

import com.booksocial.shelf.domain.ShelfStatus;

import java.time.Instant;

public record ShelfResponse(
        Long id, String bookIsbn, String title, String author,
        ShelfStatus status, Instant createdAt, Instant updatedAt
) {}
