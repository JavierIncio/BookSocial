package com.booksocial.book.service.openlibrary;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AuthorDetailResponse(
    @JsonProperty("key") String key,
    @JsonProperty("name") String name,
    @JsonProperty("bio") String bio,
    @JsonProperty("birth_date") String birthDate,
    @JsonProperty("death_date") String deathDate,
    @JsonProperty("photos") List<Long> photos,
    @JsonProperty("links") List<Link> links,
    @JsonProperty("alternate_names") List<String> alternateNames) {

        public record Link(
                @JsonProperty("title") String title,
                @JsonProperty("url") String url) {}
}