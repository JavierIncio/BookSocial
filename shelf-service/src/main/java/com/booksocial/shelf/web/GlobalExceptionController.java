package com.booksocial.shelf.web;

import com.booksocial.shelf.domain.BookNotInCatalogException;
import com.booksocial.shelf.domain.ShelfAlreadyExistsException;
import com.booksocial.shelf.domain.ShelfNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionController {
    @ExceptionHandler(ShelfNotFoundException.class)
    public ResponseEntity<Map<String,Object>> notFound(ShelfNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "not_found", "message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException ex){
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst().orElse("Invalid request");
        return ResponseEntity.badRequest().body(Map.of("error", "validation", "message", message));
    }

    @ExceptionHandler(ShelfAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> alreadyExists(ShelfAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "conflict", "message", ex.getMessage()));
    }

    @ExceptionHandler(BookNotInCatalogException.class)
    public ResponseEntity<Map<String, Object>> forbidden(BookNotInCatalogException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "unprocessable", "message", ex.getMessage()));
    }
}
