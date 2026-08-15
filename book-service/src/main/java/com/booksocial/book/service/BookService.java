package com.booksocial.book.service;

import com.booksocial.book.domain.Book;
import com.booksocial.book.domain.BookAlreadyExistsException;
import com.booksocial.book.domain.BookNotFoundException;
import com.booksocial.book.readmodel.BookReadModel;
import com.booksocial.book.readmodel.BookReadModelRepository;
import com.booksocial.book.repository.BookRepository;
import com.booksocial.book.web.dto.BookResponse;
import com.booksocial.book.web.dto.CreateBookRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BookService {
    private final BookRepository bookRepository;
    private final BookReadModelRepository readModelRepository;

    public BookService(BookRepository bookRepository, BookReadModelRepository readModelRepository) {
        this.bookRepository = bookRepository;
        this.readModelRepository = readModelRepository;
    }

    public BookResponse create(CreateBookRequest request) {
        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new BookAlreadyExistsException(request.isbn());
        }

        Book book = bookRepository.save(new Book(
                request.isbn(),
                request.title(),
                request.author(),
                request.description(),
                request.coverUrl(),
                request.publishedYear(),
                request.category())
        );

        return toResponse(upsertReadModel(book));
    }

    public BookResponse findByIsbn(String isbn) {
        BookReadModel readModel = readModelRepository.findById(isbn)
                .orElseThrow(() -> new BookNotFoundException(isbn));
        return toResponse(readModel);
    }

    public List<BookResponse> search(String q) {
        return readModelRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(q, q)
                .stream().map(this::toResponse).toList();
    }

    private BookReadModel upsertReadModel(Book book) {
        BookReadModel readModel = new BookReadModel(
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getDescription(),
                book.getCoverUrl(),
                book.getPublishedYear(),
                book.getCategory()
        );
        return readModelRepository.save(readModel);
    }

    private BookResponse toResponse(BookReadModel readModel) {
        return new BookResponse(
                readModel.getIsbn(),
                readModel.getTitle(),
                readModel.getAuthor(),
                readModel.getDescription(),
                readModel.getCoverUrl(),
                readModel.getPublishedYear(),
                readModel.getCategory(),
                readModel.getCreatedAt()
        );
    }
}
