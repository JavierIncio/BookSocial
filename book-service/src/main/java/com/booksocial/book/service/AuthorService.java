package com.booksocial.book.service;

import com.booksocial.book.domain.Author;
import com.booksocial.book.readmodel.AuthorReadModel;
import com.booksocial.book.readmodel.AuthorReadModelRepository;
import com.booksocial.book.repository.AuthorRepository;
import com.booksocial.book.service.openlibrary.AuthorDetailResponse;
import com.booksocial.book.service.openlibrary.OpenLibraryClient;
import com.booksocial.book.service.openlibrary.OpenLibraryMapper;
import com.booksocial.book.service.openlibrary.WorksResponse;
import com.booksocial.book.web.dto.AuthorResponse;
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

    public List<AuthorResponse> searchAuthors(String query) {
        List<AuthorReadModel> localAuthors = readModelRepository.findByNameContainingIgnoreCase(query);
        if (!localAuthors.isEmpty()) return localAuthors.stream().map(this::toResponse).toList();

        List<AuthorReadModel> openLibraryAuthors = openLibraryClient.searchAuthors(query)
                    .docs()
                    .stream()
                    .map(mapper::toReadModel)
                    .toList();

        return readModelRepository.saveAll(openLibraryAuthors).stream().map(this::toResponse).toList();
    }

    public AuthorResponse getAuthor(String openLibraryId) {
        AuthorReadModel cached = readModelRepository.findById(openLibraryId).orElse(null);
        if (cached != null && cached.getBio() != null && !cached.getBio().isBlank()) {
            return toResponse(cached);
        }

        AuthorDetailResponse olAuthor = openLibraryClient.getAuthor(openLibraryId);
        if (olAuthor == null) {
            return cached != null ? toResponse(cached) : null;
        }

        String photoUrl = cached != null && cached.getPhotoUrl() != null
                ? cached.getPhotoUrl()
                : mapper.coverUrl(openLibraryId);
        AuthorReadModel merged = new AuthorReadModel(openLibraryId, olAuthor.name(), olAuthor.bioText(),
                olAuthor.birthDate(), olAuthor.deathDate(), photoUrl,
                cached != null ? cached.getTopSubjects() : null,
                cached != null ? cached.getWorkCount() : null);
        readModelRepository.save(merged);
        return toResponse(merged);
    }

    public AuthorResponse getAuthorById(Long authorId) {
        Author author = authorRepository.findById(authorId).orElse(null);
        if (author == null) return null;

        String openLibraryId = author.getOpenLibraryId();
        if (openLibraryId == null || openLibraryId.isBlank()) {
            openLibraryId = resolveOpenLibraryIdByName(author.getName());
        }
        if (openLibraryId == null) {
            return new AuthorResponse(null, author.getName(), author.getBio(),
                    author.getBirthDate(), author.getDeathDate(), author.getPhotoUrl(),
                    null, author.getWorkCount());
        }
        return getAuthor(openLibraryId);
    }

    private String resolveOpenLibraryIdByName(String name) {
        List<AuthorReadModel> cached = readModelRepository.findByNameContainingIgnoreCase(name);
        String fromCache = cached.stream()
                .filter(rm -> rm.getName() != null && rm.getName().equalsIgnoreCase(name))
                .map(AuthorReadModel::getOpenLibraryId)
                .findFirst()
                .orElse(null);
        if (fromCache != null) return fromCache;

        List<AuthorReadModel> fetched = openLibraryClient.searchAuthors(name)
                .docs()
                .stream()
                .map(mapper::toReadModel)
                .toList();
        readModelRepository.saveAll(fetched);
        return fetched.stream()
                .filter(rm -> rm.getName() != null && rm.getName().equalsIgnoreCase(name))
                .map(AuthorReadModel::getOpenLibraryId)
                .findFirst()
                .orElse(null);
    }

    public WorksResponse getAuthorWorks(String openLibraryId) {
        return openLibraryClient.getWorks(openLibraryId);
    }

    public AuthorResponse createAuthor(String name) {
        Author author = authorRepository.save(new Author(name));
        return new AuthorResponse(null, author.getName(), null, null, null, null, null, null);
    }

    private AuthorResponse toResponse(AuthorReadModel rm) {
        return new AuthorResponse(
                rm.getOpenLibraryId(), rm.getName(), rm.getBio(),
                rm.getBirthDate(), rm.getDeathDate(), rm.getPhotoUrl(),
                rm.getTopSubjects(), rm.getWorkCount());
    }
}
