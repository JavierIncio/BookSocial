package com.booksocial.book.web.dto;

import java.util.List;

public record AuthorResponse(
        String openLibraryId,
        String name,
        String bio,
        String birthDate,
        String deathDate,
        String photoUrl,
        List<String> topSubjects,
        Integer workCount) {}
