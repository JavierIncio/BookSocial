package com.booksocial.review.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateReviewRequest(
        @Min(1) @Max(5) Integer rating,  // nullable → solo actualiza si se envía
        String comment) {}