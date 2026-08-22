package com.booksocial.book.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "authors")
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "open_library_id", unique = true)
    private String openLibraryId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(name = "birth_date")
    private String birthDate;

    @Column(name = "death_date")
    private String deathDate;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "top_subjects")
    private String topSubjects; // JSON serializado

    @Column(name = "work_count")
    private Integer workCount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Author() {}

    public Author(String openLibraryId, String name, String bio, String birthDate,
                  String deathDate, String photoUrl, String topSubjects, Integer workCount) {
        this.openLibraryId = openLibraryId;
        this.name = name;
        this.bio = bio;
        this.birthDate = birthDate;
        this.deathDate = deathDate;
        this.photoUrl = photoUrl;
        this.topSubjects = topSubjects;
        this.workCount = workCount;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getTopSubjects() {
        return topSubjects;
    }

    public void setTopSubjects(String topSubjects) {
        this.topSubjects = topSubjects;
    }

    public Integer getWorkCount() {
        return workCount;
    }

    public void setWorkCount(Integer workCount) {
        this.workCount = workCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
