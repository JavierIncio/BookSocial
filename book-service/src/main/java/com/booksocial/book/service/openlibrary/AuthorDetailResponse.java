package com.booksocial.book.service.openlibrary;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record AuthorDetailResponse(
    @JsonProperty("key") String key,
    @JsonProperty("name") String name,
    @JsonProperty("bio") Object bio,
    @JsonProperty("birth_date") String birthDate,
    @JsonProperty("death_date") String deathDate,
    @JsonProperty("photos") List<Long> photos,
    @JsonProperty("links") List<Link> links,
    @JsonProperty("alternate_names") List<String> alternateNames) {

        public String bioText() {
            return switch (bio) {
                case String text -> text.isBlank() ? null : text;
                case Map<?, ?> map -> {
                    Object value = map.get("value");
                    yield value == null || value.toString().isBlank() ? null : value.toString();
                }
                case null, default -> null;
            };
        }

        public record Link(
                @JsonProperty("title") String title,
                @JsonProperty("url") String url) {}
}