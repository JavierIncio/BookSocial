package com.booksocial.book.service.google;

import com.booksocial.book.domain.Book;
import org.springframework.stereotype.Component;

import static com.booksocial.book.service.google.GoogleBooksResponse.*;

@Component
public class GoogleBooksMapper {

    public Book mapToBook(Volume volume) {
        VolumeInfo info = volume.volumeInfo();
        String isbn = extractIsbn(info);
        Integer publishedYear = extractYear(info.publishedDate());
        String category = (info.categories() != null && !info.categories().isEmpty())
                ? info.categories().getFirst() : null;
        String coverUrl = (info.imageLinks() != null) ? info.imageLinks().thumbnail() : null;
        String authors = (info.authors() != null) ? String.join(", ", info.authors()) : "Unknown";

        return new Book(isbn, info.title(), authors, info.description(), coverUrl, publishedYear, category);

    }

    private String extractIsbn(VolumeInfo info) {
        if (info.industryIdentifiers() == null) return null;
        return info.industryIdentifiers().stream()
                .filter(id -> "ISBN_13".equals(id.type()))
                .map(IndustryIdentifier::identifier)
                .findFirst()
                .orElseGet(() -> info.industryIdentifiers().stream()
                        .filter(id -> "ISBN_10".equals(id.type()))
                        .map(IndustryIdentifier::identifier)
                        .findFirst().orElse(null));
    }

    private Integer extractYear(String date) {
        if (date == null || date.isEmpty()) return null;
        try {
            return Integer.parseInt(date.length() >= 4 ? date.substring(0, 4) : date);
        } catch (NumberFormatException e) { return null; }
    }


}
