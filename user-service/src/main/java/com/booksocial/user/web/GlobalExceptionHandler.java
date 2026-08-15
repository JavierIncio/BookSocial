package com.booksocial.user.web;

import com.booksocial.user.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<Map<String,Object>> notFound(ProfileNotFoundException ex){
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

    @ExceptionHandler(SelfFollowException.class)
    public ResponseEntity<Map<String, Object>> selfFollow(SelfFollowException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "bad_request", "message", ex.getMessage()));
    }

    @ExceptionHandler(AlreadyFollowingException.class)
    public ResponseEntity<Map<String, Object>> alreadyFollowing(AlreadyFollowingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "conflict", "message", ex.getMessage()));
    }

    @ExceptionHandler(NotFollowingException.class)
    public ResponseEntity<Map<String, Object>> notFollowing(NotFollowingException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "not_found", "message", ex.getMessage()));
    }
}
