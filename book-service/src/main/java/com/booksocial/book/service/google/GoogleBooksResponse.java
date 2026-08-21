package com.booksocial.book.service.google;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GoogleBooksResponse(
        @JsonProperty("totalItems") int totalItems,
        @JsonProperty("items") List<Volume> items) {

    public record Volume(
            @JsonProperty("volumeInfo") VolumeInfo volumeInfo){}

    public record VolumeInfo(
            String title,
            List<String> authors,
            @JsonProperty("publishedDate") String publishedDate,
            String description,
            @JsonProperty("categories") List<String> categories,
            @JsonProperty("imageLinks") ImageLinks imageLinks,
            @JsonProperty("industryIdentifiers") List<IndustryIdentifier> industryIdentifiers) {}

    public record ImageLinks(@JsonProperty("thumbnail") String thumbnail) {}

    public record IndustryIdentifier(
            @JsonProperty("type") String type,
            @JsonProperty("identifier") String identifier) {}
}
