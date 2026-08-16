package com.booksocial.review.web;

import com.booksocial.review.domain.BookNotInCatalogException;
import com.booksocial.review.domain.ReviewAlreadyExistsException;
import com.booksocial.review.domain.ReviewNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<Map<String,Object>> notFound(ReviewNotFoundException ex){
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

    @ExceptionHandler(ReviewAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> alreadyExists(ReviewAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "conflict", "message", ex.getMessage()));
    }

    @ExceptionHandler(BookNotInCatalogException.class)
    public ResponseEntity<Map<String, Object>> forbidden(BookNotInCatalogException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "unprocessable", "message", ex.getMessage()));
    }

}
