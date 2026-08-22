package com.booksocial.book.service.openlibrary;

import com.booksocial.book.config.OpenLibraryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OpenLibraryClient {
    private static final Logger log = LoggerFactory.getLogger(OpenLibraryClient.class);
    private final RestClient restClient;

    public OpenLibraryClient(OpenLibraryProperties props) {
        this.restClient = RestClient.builder()
                .baseUrl(props.apiUrl())
                .defaultHeader("User-Agent", props.userAgent())
                .build();
    }

    public OpenLibraryResponse searchAuthors(String query) {
        try {
            OpenLibraryResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/authors.json")
                            .queryParam("q", query)
                            .build())
                    .retrieve()
                    .body(OpenLibraryResponse.class);

            if (response == null || response.docs() == null || response.docs().isEmpty())
                return new OpenLibraryResponse(0, List.of());
            return response;
        } catch (Exception e) {
            log.error("Error fetching authors with query: {}", query, e);
            return new OpenLibraryResponse(0, List.of());
        }
    }

    public AuthorDetailResponse getAuthor(String openLibraryId) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/authors/{id}.json")
                            .build(openLibraryId))
                    .retrieve()
                    .body(AuthorDetailResponse.class);
        } catch (Exception e) {
            log.error("Error fetching author details for ID: {}", openLibraryId, e);
            return null;
        }
    }

    public WorksResponse getWorks(String openLibraryId) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/authors/{id}/works.json")
                            .build(openLibraryId))
                    .retrieve()
                    .body(WorksResponse.class);
        } catch (Exception e) {
            log.error("Error fetching works for author ID: {}", openLibraryId, e);
            return null;
        }
    }
}
