package com.booksocial.book.service;

import com.booksocial.book.domain.Author;
import com.booksocial.book.domain.Book;
import com.booksocial.book.domain.BookAlreadyExistsException;
import com.booksocial.book.domain.BookNotFoundException;
import com.booksocial.book.events.BookEventPublisher;
import com.booksocial.book.readmodel.BookReadModel;
import com.booksocial.book.readmodel.BookReadModelRepository;
import com.booksocial.book.repository.AuthorRepository;
import com.booksocial.book.repository.BookRepository;
import com.booksocial.book.service.google.GoogleBooksClient;
import com.booksocial.book.service.google.GoogleBooksMapper;
import com.booksocial.book.service.google.GoogleBooksResponse;
import com.booksocial.book.web.dto.BookResponse;
import com.booksocial.book.web.dto.CreateBookRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@Service
@Transactional
public class BookService {
    private final BookRepository bookRepository;
    private final BookReadModelRepository readModelRepository;
    private final BookEventPublisher bookEventPublisher;
    private final GoogleBooksClient googleBooksClient;
    private final GoogleBooksMapper googleBooksMapper;
    private final AuthorRepository authorRepository;

    public BookService(BookRepository bookRepository,
                       BookReadModelRepository readModelRepository,
                       BookEventPublisher bookEventPublisher,
                       GoogleBooksClient googleBooksClient,
                       GoogleBooksMapper googleBooksMapper, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.readModelRepository = readModelRepository;
        this.bookEventPublisher = bookEventPublisher;
        this.googleBooksClient = googleBooksClient;
        this.googleBooksMapper = googleBooksMapper;
        this.authorRepository = authorRepository;
    }

    public BookResponse create(CreateBookRequest request) {
        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new BookAlreadyExistsException(request.isbn());
        }

        Book book = bookRepository.save(new Book(
                request.isbn(),
                request.title(),
                Long.valueOf(request.authorId()),
                request.description(),
                request.coverUrl(),
                request.publishedYear(),
                request.category())
        );

        Author author = resolveAuthor(book.getAuthorId());

        BookResponse response = toResponse(upsertReadModel(book));
        bookEventPublisher.publishBookCreated(book.getIsbn(), book.getTitle(), author.getName(), author.getId().toString());
        return response;
    }

    public BookResponse findByIsbn(String isbn) {
        return readModelRepository.findById(isbn)
            .map(this::toResponse)
            .orElseGet(() -> {
                GoogleBooksResponse.Volume volume =
                        googleBooksClient.findByIsbn(isbn);

                if (volume == null)
                    throw new BookNotFoundException(isbn);

                Book book = googleBooksMapper.mapToBook(volume);
                BookResponse response = toResponse(upsertReadModel(book));

                Author author = resolveAuthor(book.getAuthorId());
                bookEventPublisher.publishBookCreated(book.getIsbn(), book.getTitle(), author.getName(), author.getId().toString());

                return response;
            });
    }

    public List<BookResponse> search(String q) {
        return readModelRepository.findByTitleContainingIgnoreCaseOrAuthorNameContainingIgnoreCase(q, q)
                .stream().map(this::toResponse).toList();
    }

    public List<BookResponse> searchExternal(String q) {
        List<BookResponse> dbResults = this.search(q);

        List<BookResponse> googleResults = googleBooksClient.search(q).stream()
                .map(volume -> {
                    GoogleBooksResponse.VolumeInfo info = volume.volumeInfo();
                    String authorName = (info.authors() != null && !info.authors().isEmpty())
                            ? info.authors().getFirst() : "Unknown";
                    String isbn = null;
                    if (info.industryIdentifiers() != null) {
                        isbn = info.industryIdentifiers().stream()
                                .filter(id -> "ISBN_13".equals(id.type()))
                                .map(GoogleBooksResponse.IndustryIdentifier::identifier)
                                .findFirst()
                                .orElseGet(() -> info.industryIdentifiers().stream()
                                        .filter(id -> "ISBN_10".equals(id.type()))
                                        .map(GoogleBooksResponse.IndustryIdentifier::identifier)
                                        .findFirst().orElse(null));
                    }
                    Integer publishedYear = null;
                    if (info.publishedDate() != null && !info.publishedDate().isEmpty()) {
                        try { publishedYear = Integer.parseInt(info.publishedDate().substring(0, 4)); }
                        catch (NumberFormatException ignored) {}
                    }
                    String category = (info.categories() != null && !info.categories().isEmpty())
                            ? info.categories().getFirst() : null;
                    String coverUrl = (info.imageLinks() != null) ? info.imageLinks().thumbnail() : null;

                    BookReadModel readModel = new BookReadModel(
                            isbn, info.title(), authorName, null,
                            info.description(), coverUrl, publishedYear, category);
                    return toResponse(readModel);
                })
                .toList();

        return Stream.concat(dbResults.stream(), googleResults.stream())
                .toList();
    }

    private BookReadModel upsertReadModel(Book book) {
        Author author = resolveAuthor(book.getAuthorId());
        BookReadModel readModel = new BookReadModel(
                book.getIsbn(),
                book.getTitle(),
                author.getName(),
                book.getAuthorId().toString(),
                book.getDescription(),
                book.getCoverUrl(),
                book.getPublishedYear(),
                book.getCategory()
        );
        return readModelRepository.save(readModel);
    }

    private BookResponse toResponse(BookReadModel readModel) {
        Author author = resolveAuthor(Long.valueOf(readModel.getAuthorId()));
        return new BookResponse(
                readModel.getIsbn(),
                readModel.getTitle(),
                author.getName(),
                readModel.getAuthorId(),
                readModel.getDescription(),
                readModel.getCoverUrl(),
                readModel.getPublishedYear(),
                readModel.getCategory(),
                readModel.getCreatedAt()
        );
    }

    private Author resolveAuthor(Long authorId) {
        return authorRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Author not found with ID: " + authorId));
    }
}
