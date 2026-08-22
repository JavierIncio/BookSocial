package com.booksocial.book.service.google;

import com.booksocial.book.domain.Author;
import com.booksocial.book.domain.Book;
import com.booksocial.book.repository.AuthorRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.booksocial.book.service.google.GoogleBooksResponse.*;

@Component
public class GoogleBooksMapper {

    private final AuthorRepository authorRepository;

    public GoogleBooksMapper(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public Book mapToBook(Volume volume) {
        VolumeInfo info = volume.volumeInfo();
        String isbn = extractIsbn(info);
        Integer publishedYear = extractYear(info.publishedDate());
        String category = (info.categories() != null && !info.categories().isEmpty())
                ? info.categories().getFirst() : null;
        String coverUrl = (info.imageLinks() != null) ? info.imageLinks().thumbnail() : null;
        String authorName = (info.authors() != null && !info.authors().isEmpty())
                ? info.authors().getFirst() : "Unknown";

        Author author = findOrCreateAuthor(authorName);

        return new Book(isbn, info.title(), author.getId(), info.description(), coverUrl, publishedYear, category);
    }

    private Author findOrCreateAuthor(String name) {
        Optional<Author> existing = authorRepository.findByNameContainingIgnoreCase(name)
                .stream().findFirst();
        if (existing.isPresent()) return existing.get();

        Author newAuthor = new Author();
        newAuthor.setName(name);
        return authorRepository.save(newAuthor);
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
