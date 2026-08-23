package com.booksocial.shelf.service;


import com.booksocial.shelf.domain.*;
import com.booksocial.shelf.readmodel.BookRefReadModel;
import com.booksocial.shelf.readmodel.BookRefReadModelRepository;
import com.booksocial.shelf.readmodel.ShelfReadModel;
import com.booksocial.shelf.readmodel.ShelfReadModelRepository;
import com.booksocial.shelf.repository.ShelfRepository;
import com.booksocial.shelf.web.dto.CreateShelfRequest;
import com.booksocial.shelf.web.dto.ShelfResponse;
import com.booksocial.shelf.web.dto.UpdateShelfRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class ShelfService {

    private final BookRefReadModelRepository bookRefRepository;
    private final ShelfRepository shelfRepository;
    private final ShelfReadModelRepository readModelRepository;

    public ShelfService(BookRefReadModelRepository bookRefRepository,
                        ShelfRepository shelfRepository,
                        ShelfReadModelRepository readModelRepository) {
        this.bookRefRepository = bookRefRepository;
        this.shelfRepository = shelfRepository;
        this.readModelRepository = readModelRepository;
    }

    public ShelfResponse create(CreateShelfRequest req, Long userId) {
        BookRefReadModel bookRef = bookRefRepository.findById(req.bookIsbn())
                .orElseThrow(() -> new BookNotInCatalogException(req.bookIsbn()));

        if (shelfRepository.existsByUserIdAndBookIsbn(userId, req.bookIsbn()))
            throw new ShelfAlreadyExistsException(req.bookIsbn(), userId);

        Shelf shelf = new Shelf(req.bookIsbn(), userId, req.status());
        shelfRepository.save(shelf);

        ShelfReadModel readModel = new ShelfReadModel(shelf, bookRef);
        readModelRepository.save(readModel);

        return toResponse(readModel);
    }

    public ShelfResponse updateStatus(String isbn, Long userId, UpdateShelfRequest req){

        BookRefReadModel bookRef = bookRefRepository.findById(isbn)
                .orElseThrow(() -> new BookNotInCatalogException(isbn));

        Shelf shelf = shelfRepository.findByUserIdAndBookIsbn(userId, isbn)
                .orElseThrow(() -> new ShelfNotFoundException(isbn,  userId));

        if (shelf.getStatus() == req.status()) {
            return toResponse(readModelRepository.save(new ShelfReadModel(shelf, bookRef)));
        }

        shelf.setStatus(req.status());
        shelf.setUpdatedAt(Instant.now());
        shelfRepository.save(shelf);

        ShelfReadModel readModel = readModelRepository.save(new ShelfReadModel(shelf, bookRef));

        return toResponse(readModel);
    }

    public void delete(String isbn, Long userId) {

        Shelf shelf = shelfRepository.findByUserIdAndBookIsbn(userId, isbn)
                .orElseThrow(() -> new ShelfNotFoundException(isbn,  userId));

        shelfRepository.delete(shelf);
        readModelRepository.deleteById(userId + ":" + isbn);
    }

    public List<ShelfResponse> listByUser(Long userId) {
        return readModelRepository.findAllByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ShelfResponse> listByBookIsbn(String bookIsbn) {
        return readModelRepository.findAllByBookIsbn(bookIsbn)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ShelfResponse toResponse(ShelfReadModel rm){
        return new ShelfResponse(
                null, rm.getBookIsbn(), rm.getTitle(), rm.getAuthorName(), rm.getAuthorId(),
                rm.getStatus(), rm.getCreatedAt(), rm.getUpdatedAt()
        );
    }
}
