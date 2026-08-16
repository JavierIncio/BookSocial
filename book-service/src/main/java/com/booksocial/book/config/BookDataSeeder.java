package com.booksocial.book.config;

import com.booksocial.book.domain.Book;
import com.booksocial.book.events.BookEventPublisher;
import com.booksocial.book.readmodel.BookReadModel;
import com.booksocial.book.readmodel.BookReadModelRepository;
import com.booksocial.book.repository.BookRepository;
import com.booksocial.book.web.dto.CreateBookRequest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.seed.books", havingValue = "true", matchIfMissing = true)
public class BookDataSeeder implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final BookReadModelRepository readModelRepository;
    private final BookEventPublisher bookEventPublisher;

    private static final List<CreateBookRequest> books = List.of(
            new CreateBookRequest(
                    "9780132350884",
                    "Clean Code",
                    "Robert C. Martin",
                    "A handbook of agile software craftsmanship and clean coding practices.",
                    "https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg",
                    2008,
                    "Programming"
            ),
            new CreateBookRequest(
                    "9781617294945",
                    "Spring in Action",
                    "Craig Walls",
                    "A practical guide to developing applications with Spring and Spring Boot.",
                    "https://covers.openlibrary.org/b/isbn/9781617294945-L.jpg",
                    2022,
                    "Programming"
            ),
            new CreateBookRequest(
                    "9780134685991",
                    "Effective Java",
                    "Joshua Bloch",
                    "Best practices and design patterns for writing robust Java applications.",
                    "https://covers.openlibrary.org/b/isbn/9780134685991-L.jpg",
                    2018,
                    "Programming"
            ),
            new CreateBookRequest(
                    "9781491950357",
                    "Designing Data-Intensive Applications",
                    "Martin Kleppmann",
                    "A detailed exploration of the principles behind reliable and scalable data systems.",
                    "https://covers.openlibrary.org/b/isbn/9781491950357-L.jpg",
                    2017,
                    "Technology"
            ),
            new CreateBookRequest(
                    "9780134494166",
                    "Clean Architecture",
                    "Robert C. Martin",
                    "A guide to designing software systems that are maintainable, flexible, and independent.",
                    "https://covers.openlibrary.org/b/isbn/9780134494166-L.jpg",
                    2017,
                    "Programming"
            ),
            new CreateBookRequest(
                    "9780596007126",
                    "Head First Design Patterns",
                    "Eric Freeman",
                    "An accessible introduction to object-oriented design patterns and principles.",
                    "https://covers.openlibrary.org/b/isbn/9780596007126-L.jpg",
                    2004,
                    "Programming"
            ),
            new CreateBookRequest(
                    "9780262033848",
                    "Introduction to Algorithms",
                    "Thomas H. Cormen",
                    "A comprehensive introduction to algorithms, data structures, and algorithmic problem solving.",
                    "https://covers.openlibrary.org/b/isbn/9780262033848-L.jpg",
                    2009,
                    "Computer Science"
            ),
            new CreateBookRequest(
                    "9780201633610",
                    "Design Patterns",
                    "Erich Gamma",
                    "A classic reference covering reusable solutions to common object-oriented design problems.",
                    "https://covers.openlibrary.org/b/isbn/9780201633610-L.jpg",
                    1994,
                    "Programming"
            )
    );

    public BookDataSeeder(BookRepository bookRepository, BookReadModelRepository readModelRepository, BookEventPublisher bookEventPublisher) {
        this.bookRepository = bookRepository;
        this.readModelRepository = readModelRepository;
        this.bookEventPublisher = bookEventPublisher;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (bookRepository.count() > 0) return;

        for (CreateBookRequest book : books) {
            bookRepository.save(new Book(
                    book.isbn(), book.title(), book.author(), book.description(),
                    book.coverUrl(), book.publishedYear(), book.category()
            ));
            readModelRepository.save(new BookReadModel(
                    book.isbn(), book.title(), book.author(), book.description(),
                    book.coverUrl(), book.publishedYear(), book.category()
            ));
            bookEventPublisher.publishBookCreated(book.isbn(), book.title(), book.author());
        }

    }

}

