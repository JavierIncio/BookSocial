package com.booksocial.shelf.web;

import com.booksocial.shelf.service.ShelfService;
import com.booksocial.shelf.web.dto.CreateShelfRequest;
import com.booksocial.shelf.web.dto.ShelfResponse;
import com.booksocial.shelf.web.dto.UpdateShelfRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shelves")
public class ShelfController {

    private final ShelfService shelfService;

    public ShelfController(ShelfService shelfService) {
        this.shelfService = shelfService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShelfResponse create(@Valid @RequestBody CreateShelfRequest request,
                                @RequestHeader("X-User-Id") Long userId) {
        return shelfService.create(request, userId);
    }

    @PutMapping("/{isbn}")
    public ShelfResponse updateStatus(@PathVariable String isbn,
                                      @Valid @RequestBody UpdateShelfRequest request,
                                      @RequestHeader("X-User-Id") Long userId) {
        return shelfService.updateStatus(isbn, userId, request);
    }

    @DeleteMapping("/{isbn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String isbn,
                       @RequestHeader("X-User-Id") Long userId) {
        shelfService.delete(isbn, userId);
    }

    @GetMapping
    public List<ShelfResponse> list(@RequestHeader("X-User-Id") Long userId) {
        return shelfService.listByUser(userId);
    }

    @GetMapping("/users/{userId}")
    public List<ShelfResponse> listByUserPublic(@PathVariable Long userId) {
        return shelfService.listByUser(userId);
    }

    @GetMapping("/{isbn}")
    public List<ShelfResponse> listByIsbn(@PathVariable String isbn) {
        return shelfService.listByBookIsbn(isbn);
    }
}
