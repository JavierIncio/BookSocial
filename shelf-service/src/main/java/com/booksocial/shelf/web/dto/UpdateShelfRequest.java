package com.booksocial.shelf.web.dto;

import com.booksocial.shelf.domain.ShelfStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateShelfRequest(@NotNull ShelfStatus status) {}
