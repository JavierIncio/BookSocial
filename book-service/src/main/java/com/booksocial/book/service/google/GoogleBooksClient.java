package com.booksocial.book.service.google;

import com.booksocial.book.config.GoogleBooksProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
public class GoogleBooksClient {
    private static final Logger log = LoggerFactory.getLogger(GoogleBooksClient.class);
    private final RestClient restClient;
    private final GoogleBooksProperties props;

    public GoogleBooksClient(GoogleBooksProperties props) {
        this.props = props;
        this.restClient = RestClient.builder().baseUrl(props.apiUrl()).build();
    }

    public List<GoogleBooksResponse.Volume> search(String query) {
        try {
            GoogleBooksResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/volumes")
                            .queryParam("q", query)
                            .queryParam("maxResults", 10)
                            .queryParamIfPresent("key", Optional.ofNullable(props.apiKey()))
                            .build())
                    .retrieve()
                    .body(GoogleBooksResponse.class);

            if (response == null || response.items() == null) return List.of();
            return response.items();
        } catch (Exception e) {
            log.error("Error searching for books with query: {}", query, e);
            return List.of();
        }
    }

    public GoogleBooksResponse.Volume findByIsbn(String isbn) {
        try {
            GoogleBooksResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/volumes")
                            .queryParam("q", "isbn:" + isbn)
                            .queryParam("maxResults", 1)
                            .queryParamIfPresent("key", Optional.ofNullable(props.apiKey()))
                            .build())
                    .retrieve()
                    .body(GoogleBooksResponse.class);

            if (response == null || response.items() == null || response.items().isEmpty()) return null;
            return response.items().stream().findFirst().orElse(null);

        } catch (Exception e) {
            log.error("Error finding book by ISBN: {}", isbn, e);
            return null;
        }
    }
}
