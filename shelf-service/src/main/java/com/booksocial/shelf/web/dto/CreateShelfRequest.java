package com.booksocial.shelf.web.dto;

import com.booksocial.shelf.domain.ShelfStatus;
import jakarta.validation.constraints.NotBlank;

public record CreateShelfRequest(@NotBlank String bookIsbn, ShelfStatus status) {
    public CreateShelfRequest {
        if (status == null)
            status = ShelfStatus.WANTS_TO_READ;
    }
}
