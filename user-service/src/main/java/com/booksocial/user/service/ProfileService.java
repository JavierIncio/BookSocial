package com.booksocial.user.service;

import com.booksocial.user.domain.Profile;
import com.booksocial.user.readmodel.ProfileReadModel;
import com.booksocial.user.readmodel.ProfileReadModelRepository;
import com.booksocial.user.repository.ProfileRepository;
import com.booksocial.user.web.dto.ProfileResponse;
import com.booksocial.user.web.dto.UpdateProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProfileService {
    private final ProfileRepository profileRepository;
    private final ProfileReadModelRepository readModelRepository;

    public ProfileService(ProfileRepository profileRepository, ProfileReadModelRepository readModelRepository) {
        this.profileRepository = profileRepository;
        this.readModelRepository = readModelRepository;
    }

    public ProfileResponse getOrCreate(Long userId, String email) {
        Profile profile = findOrCreateProfile(userId, email);
        return toResponse(upsertReadModel(profile));
    }

    public ProfileResponse update(Long userId, String email, UpdateProfileRequest request) {
        Profile profile = findOrCreateProfile(userId, email);

        updateProfile(profile, request);
        profile.touch();

        profileRepository.save(profile);
        return toResponse(upsertReadModel(profile));
    }

    public ProfileResponse getByUserId(Long userId) {
        ProfileReadModel readModel = readModelRepository.findByUserId(userId)
                .orElseGet(() -> upsertReadModel(
                        profileRepository.findByUserId(userId)
                                .orElseGet(() -> createSyntheticProfile(userId))));

        return toResponse(readModel);
    }

    private Profile createSyntheticProfile(Long userId) {
        Profile profile = new Profile();
        profile.setUserId(userId);
        profile.setEmail("user-" + userId + "@booksocial.local");
        profile.setDisplayName("user-" + userId);
        return profileRepository.save(profile);
    }

    private Profile findOrCreateProfile(Long userId, String email) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> createProfile(userId, email));
    }

    private Profile createProfile(Long userId, String email) {
        Profile profile = new Profile();
        profile.setUserId(userId);
        profile.setEmail(email);
        return profileRepository.save(profile);
    }

    private void updateProfile(Profile profile, UpdateProfileRequest request) {
        if (request.displayName() != null)
            profile.setDisplayName(request.displayName());

        if (request.bio() != null)
            profile.setBio(request.bio());

        if (request.location() != null)
            profile.setLocation(request.location());

        if (request.avatarUrl() != null)
            profile.setAvatarUrl(request.avatarUrl());
    }

    private ProfileReadModel upsertReadModel(Profile profile) {
        ProfileReadModel readModel = readModelRepository.findByUserId(profile.getUserId())
                .orElseGet(() -> new ProfileReadModel(profile.getUserId(), profile.getEmail()));
        readModel.setUserId(profile.getUserId());
        readModel.setEmail(profile.getEmail());
        readModel.setDisplayName(deriveDisplayName(profile.getDisplayName(), profile.getEmail()));
        readModel.setBio(profile.getBio());
        readModel.setLocation(profile.getLocation());
        readModel.setAvatarUrl(profile.getAvatarUrl());
        readModel.setUpdatedAt(profile.getUpdatedAt());
        return readModelRepository.save(readModel);
    }

    private String deriveDisplayName(String displayName, String email) {
        if (displayName != null && !displayName.isBlank()) return displayName;
        if (email != null) {
            int at = email.indexOf('@');
            if (at > 0) return email.substring(0, at);
        }
        return displayName;
    }

    private ProfileResponse toResponse(ProfileReadModel rm) {
        return new ProfileResponse(
                rm.getUserId(), rm.getEmail(),
                deriveDisplayName(rm.getDisplayName(), rm.getEmail()),
                rm.getBio(),
                rm.getLocation(), rm.getAvatarUrl(), rm.getFollowersCount(),
                rm.getFollowingCount(), rm.getPostsCount(), rm.getCreatedAt(), rm.getUpdatedAt()
        );
    }
}
