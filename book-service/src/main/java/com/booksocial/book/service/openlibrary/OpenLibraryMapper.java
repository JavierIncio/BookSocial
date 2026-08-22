package com.booksocial.book.service.openlibrary;

import com.booksocial.book.readmodel.AuthorReadModel;
import org.springframework.stereotype.Component;

import static com.booksocial.book.service.openlibrary.OpenLibraryResponse.*;

@Component
public class OpenLibraryMapper {

    public AuthorReadModel toReadModel(AuthorDoc doc) {
        String openLibraryId = extractKey(doc.key());
        String photoUrl = coverUrl(openLibraryId);

        return new AuthorReadModel(
                openLibraryId,
                doc.name(),
                null, // Bio is not available in the search response
                doc.birthDate(),
                doc.deathDate(),
                photoUrl,
                doc.topSubjects(),
                doc.workCount()
        );
    }

    public String coverUrl(String openLibraryId) {
        return String.format("https://covers.openlibrary.org/a/olid/%s-L.jpg", openLibraryId);
    }

    public String extractKey(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
