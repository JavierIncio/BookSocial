package com.booksocial.book.web;

import com.booksocial.book.domain.ForbiddenException;
import com.booksocial.book.service.BookService;
import com.booksocial.book.web.dto.BookResponse;
import com.booksocial.book.web.dto.CreateBookRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse createBook(@RequestHeader(value = "X-User-Roles", required = false) String roles,
                                   @Valid @RequestBody CreateBookRequest request) {
        if (!isAdmin(roles))
            throw new ForbiddenException("ADMIN required");
        return bookService.create(request);
    }

    @GetMapping("/{isbn}")
    public BookResponse getBook(@PathVariable String isbn) {
        return bookService.findByIsbn(isbn);
    }

    @GetMapping("/search")
    public List<BookResponse> searchBooks(@RequestParam String q) {
        return bookService.search(q);
    }

    @GetMapping("/search/full")
    public List<BookResponse> searchFull(@RequestParam String q) {
        return bookService.searchExternal(q);
    }

    private boolean isAdmin(String roles) {
        return roles != null && Arrays.stream(roles.split(",")).map(String::trim).anyMatch("ADMIN"::equals);
    }
}
