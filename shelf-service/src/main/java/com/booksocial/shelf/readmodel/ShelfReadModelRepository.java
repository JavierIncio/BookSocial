package com.booksocial.shelf.readmodel;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ShelfReadModelRepository extends MongoRepository<ShelfReadModel,String> {
    List<ShelfReadModel> findAllByUserId(Long userId);
    List<ShelfReadModel> findAllByBookIsbn(String bookIsbn);
}
