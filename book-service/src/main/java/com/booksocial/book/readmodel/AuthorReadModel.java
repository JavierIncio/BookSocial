package com.booksocial.book.readmodel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "authors")
public class AuthorReadModel {
    @Id
    private String openLibraryId;
    private String name;
    private String bio;
    private String birthDate;
    private String deathDate;
    private String photoUrl;
    private List<String> topSubjects;
    private Integer workCount;
    private Instant cachedAt;

    public AuthorReadModel() {}

    public AuthorReadModel(String openLibraryId, String name, String bio,
                           String birthDate, String deathDate, String photoUrl,
                           List<String> topSubjects, Integer workCount) {
        this.openLibraryId = openLibraryId;
        this.name = name;
        this.bio = bio;
        this.birthDate = birthDate;
        this.deathDate = deathDate;
        this.photoUrl = photoUrl;
        this.topSubjects = topSubjects;
        this.workCount = workCount;
        this.cachedAt = Instant.now();
    }

    public String getOpenLibraryId() {
        return openLibraryId;
    }

    public void setOpenLibraryId(String openLibraryId) {
        this.openLibraryId = openLibraryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getDeathDate() {
        return deathDate;
    }

    public void setDeathDate(String deathDate) {
        this.deathDate = deathDate;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public List<String> getTopSubjects() {
        return topSubjects;
    }

    public void setTopSubjects(List<String> topSubjects) {
        this.topSubjects = topSubjects;
    }

    public Integer getWorkCount() {
        return workCount;
    }

    public void setWorkCount(Integer workCount) {
        this.workCount = workCount;
    }

    public Instant getCachedAt() {
        return cachedAt;
    }

    public void setCachedAt(Instant cachedAt) {
        this.cachedAt = cachedAt;
    }
}
