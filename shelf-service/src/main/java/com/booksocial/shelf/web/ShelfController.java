package com.booksocial.shelf.web;

import com.booksocial.shelf.service.ShelfService;
import com.booksocial.shelf.web.dto.CreateShelfRequest;
import com.booksocial.shelf.web.dto.ShelfResponse;
import com.booksocial.shelf.web.dto.UpdateShelfRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ShelfResponse> create(@Valid @RequestBody CreateShelfRequest request,
                                                @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shelfService.create(request, userId));
    }

    @PutMapping("/{isbn}")
    public ResponseEntity<ShelfResponse> updateStatus(@PathVariable String isbn,
                                                           @Valid @RequestBody UpdateShelfRequest request,
                                                           @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(shelfService.updateStatus(isbn, userId, request));
    }

    @DeleteMapping("/{isbn}")
    public ResponseEntity<Void> delete(@PathVariable String isbn,
                                            @RequestHeader("X-User-Id") Long userId) {
        shelfService.delete(isbn, userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    public ResponseEntity<List<ShelfResponse>> list(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(shelfService.listByUser(userId));
    }


}
