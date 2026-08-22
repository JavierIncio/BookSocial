package com.booksocial.book.service;

import com.booksocial.book.domain.Author;
import com.booksocial.book.readmodel.AuthorReadModel;
import com.booksocial.book.readmodel.AuthorReadModelRepository;
import com.booksocial.book.repository.AuthorRepository;
import com.booksocial.book.service.openlibrary.AuthorDetailResponse;
import com.booksocial.book.service.openlibrary.OpenLibraryClient;
import com.booksocial.book.service.openlibrary.OpenLibraryMapper;
import com.booksocial.book.service.openlibrary.WorksResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AuthorService {
    private final AuthorRepository authorRepository;
    private final AuthorReadModelRepository readModelRepository;
    private final OpenLibraryClient openLibraryClient;
    private final OpenLibraryMapper mapper;

    public AuthorService(AuthorRepository authorRepository,
                         AuthorReadModelRepository readModelRepository,
                         OpenLibraryClient openLibraryClient,
                         OpenLibraryMapper mapper) {
        this.authorRepository = authorRepository;
        this.readModelRepository = readModelRepository;
        this.openLibraryClient = openLibraryClient;
        this.mapper = mapper;
    }

    public List<AuthorReadModel> searchAuthors(String query) {
        List<AuthorReadModel> localAuthors = readModelRepository.findByNameContainingIgnoreCase(query);
        if (!localAuthors.isEmpty()) return localAuthors;

        List<AuthorReadModel> openLibraryAuthors = openLibraryClient.searchAuthors(query)
                    .docs()
                    .stream()
                    .map(mapper::toReadModel)
                    .toList();

        return readModelRepository.saveAll(openLibraryAuthors);
    }

    public AuthorReadModel getAuthor(String openLibraryId) {
        AuthorReadModel existingAuthor = readModelRepository.findById(openLibraryId).orElse(null);
        if (existingAuthor != null) return existingAuthor;

        AuthorDetailResponse olAuthor = openLibraryClient.getAuthor(openLibraryId);
        if (olAuthor == null) return null;

        String photoUrl = mapper.coverUrl(openLibraryId);

        authorRepository.save(
                new Author(openLibraryId, olAuthor.name(), olAuthor.bio(),
                        olAuthor.birthDate(), olAuthor.deathDate(),
                        photoUrl, null, null)
        );

        return readModelRepository.save(
                new AuthorReadModel(openLibraryId, olAuthor.name(), olAuthor.bio(),
                        olAuthor.birthDate(), olAuthor.deathDate(),
                        photoUrl, null, null)
        );
    }

    public WorksResponse getAuthorWorks(String openLibraryId) {
        return openLibraryClient.getWorks(openLibraryId);
    }

    public Author createAuthor(String name) {
        return authorRepository.save(new Author(name));
    }
}
