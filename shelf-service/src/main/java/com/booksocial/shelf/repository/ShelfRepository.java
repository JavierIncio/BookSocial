package com.booksocial.shelf.repository;

import com.booksocial.shelf.domain.Shelf;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShelfRepository extends JpaRepository<Shelf, Long> {
    boolean existsByUserIdAndBookIsbn(Long userId, String bookIsbn);
    Optional<Shelf> findByUserIdAndBookIsbn(Long userId, String bookIsbn);
}
