package com.booksocial.book.web;

import com.booksocial.book.domain.Author;
import com.booksocial.book.domain.ForbiddenException;
import com.booksocial.book.readmodel.AuthorReadModel;
import com.booksocial.book.service.AuthorService;
import com.booksocial.book.service.openlibrary.WorksResponse;
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
    public List<AuthorReadModel> searchAuthors(@RequestParam String q) {
        return authorService.searchAuthors(q);
    }

    @GetMapping("/{openLibraryId}")
    public AuthorReadModel getAuthor(@PathVariable String openLibraryId) {
        return authorService.getAuthor(openLibraryId);
    }

    @GetMapping("/{openLibraryId}/works")
    public WorksResponse getAuthorWorks(@PathVariable String openLibraryId) {
        return authorService.getAuthorWorks(openLibraryId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Author createAuthor(
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
