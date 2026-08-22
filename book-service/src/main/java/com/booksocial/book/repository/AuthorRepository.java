package com.booksocial.book.repository;

import com.booksocial.book.domain.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findByOpenLibraryId(String openLibraryId);
    List<Author> findByNameContainingIgnoreCase(String name);
}
