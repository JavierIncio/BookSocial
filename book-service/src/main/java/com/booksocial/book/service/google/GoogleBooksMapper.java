package com.booksocial.book.service.google;

import com.booksocial.book.domain.Author;
import com.booksocial.book.domain.Book;
import com.booksocial.book.readmodel.BookReadModel;
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
        Author author = findOrCreateAuthor(extractAuthorName(info));
        return new Book(extractIsbn(info), info.title(), author.getId(),
                info.description(), extractCoverUrl(info), extractYear(info.publishedDate()), extractCategory(info));
    }

    public BookReadModel toReadModel(Volume volume) {
        VolumeInfo info = volume.volumeInfo();
        return new BookReadModel(extractIsbn(info), info.title(), extractAuthorName(info), null,
                info.description(), extractCoverUrl(info), extractYear(info.publishedDate()), extractCategory(info));
    }

    public Author findOrCreateAuthor(String name) {
        Optional<Author> existing = authorRepository.findByNameContainingIgnoreCase(name)
                .stream().findFirst();
        return existing.orElseGet(() -> authorRepository.save(new Author(name)));
    }

    public String extractIsbn(VolumeInfo info) {
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

    public Integer extractYear(String date) {
        if (date == null || date.isEmpty()) return null;
        try {
            return Integer.parseInt(date.length() >= 4 ? date.substring(0, 4) : date);
        } catch (NumberFormatException e) { return null; }
    }

    public String extractAuthorName(VolumeInfo info) {
        return (info.authors() != null && !info.authors().isEmpty())
                ? info.authors().getFirst() : "Unknown";
    }

    public String extractCategory(VolumeInfo info) {
        return (info.categories() != null && !info.categories().isEmpty())
                ? info.categories().getFirst() : null;
    }

    public String extractCoverUrl(VolumeInfo info) {
        return (info.imageLinks() != null) ? info.imageLinks().thumbnail() : null;
    }
}
