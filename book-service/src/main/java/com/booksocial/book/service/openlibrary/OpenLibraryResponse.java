package com.booksocial.book.service.openlibrary;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OpenLibraryResponse(
        @JsonProperty("numFound") int numFound,
        @JsonProperty("docs") List<AuthorDoc> docs) {

    public record AuthorDoc(
            @JsonProperty("key") String key,
            @JsonProperty("name") String name,
            @JsonProperty("birth_date") String birthDate,
            @JsonProperty("death_date") String deathDate,
            @JsonProperty("top_work") String topWork,
            @JsonProperty("work_count") int workCount,
            @JsonProperty("top_subjects") List<String> topSubjects
    ) {}
}
