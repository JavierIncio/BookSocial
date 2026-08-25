package com.booksocial.book.web;

import com.booksocial.book.domain.ForbiddenException;
import com.booksocial.book.service.AuthorService;
import com.booksocial.book.service.openlibrary.WorksResponse;
import com.booksocial.book.web.dto.AuthorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/authors")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping("/search")
    public List<AuthorResponse> searchAuthors(@RequestParam String q) {
        return authorService.searchAuthors(q);
    }

    @GetMapping("/id/{authorId}")
    public AuthorResponse getAuthorById(@PathVariable Long authorId) {
        return authorService.getAuthorById(authorId);
    }

    @GetMapping("/{openLibraryId}")
    public AuthorResponse getAuthor(@PathVariable String openLibraryId) {
        return authorService.getAuthor(openLibraryId);
    }

    @GetMapping("/{openLibraryId}/works")
    public WorksResponse getAuthorWorks(@PathVariable String openLibraryId) {
        return authorService.getAuthorWorks(openLibraryId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorResponse createAuthor(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestBody String name
    ) {
        if (!isAdmin(roles)) throw new ForbiddenException("ADMIN required");
        return authorService.createAuthor(name);
    }

    private boolean isAdmin(String roles) {
        return roles != null && Arrays.stream(roles.split(",")).map(String::trim).anyMatch("ADMIN"::equals);
    }
}
