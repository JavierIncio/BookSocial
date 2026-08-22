package com.booksocial.book.service.openlibrary;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record WorksResponse(
        @JsonProperty("size") int size,
        @JsonProperty("entries") List<WorkEntry> entries) {

    public record WorkEntry(
            @JsonProperty("key") String key,
            @JsonProperty("title") String title,
            @JsonProperty("covers") List<Long> covers,
            @JsonProperty("authors") List<AuthorRole> authors) {

        public record AuthorRole(
                @JsonProperty("author") AuthorRef author) {}

        public record AuthorRef(
                @JsonProperty("key") String key) {}
    }
}