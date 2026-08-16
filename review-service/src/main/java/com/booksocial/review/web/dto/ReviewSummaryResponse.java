package com.booksocial.review.web.dto;

public record ReviewSummaryResponse(
        String bookIsbn, long ratingCount, double averageRating) {}