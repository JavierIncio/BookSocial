package com.booksocial.user.service;

import com.booksocial.user.domain.Profile;
import com.booksocial.user.readmodel.ProfileReadModel;
import com.booksocial.user.readmodel.ProfileReadModelRepository;
import com.booksocial.user.repository.ProfileRepository;
import com.booksocial.user.web.dto.ProfileResponse;
import com.booksocial.user.web.dto.UpdateProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
                .orElseGet(() -> {
                    Profile profile = profileRepository.findByUserId(userId).orElse(null);
                    if (profile == null) {
                        return placeholderReadModel(userId);
                    }
                    return upsertReadModel(profile);
                });

        if (isSyntheticEmail(readModel.getEmail())) {
            readModel.setEmail(null);
            readModelRepository.save(readModel);
        }

        return toResponse(readModel);
    }

    public List<ProfileResponse> searchProfiles(String query) {
        return readModelRepository.findByDisplayNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ProfileReadModel placeholderReadModel(Long userId) {
        ProfileReadModel readModel = new ProfileReadModel(userId, null);
        readModel.setDisplayName("user-" + userId);
        return readModel;
    }

    private Profile findOrCreateProfile(Long userId, String email) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> createProfile(userId, email));

        if (email != null && !email.isBlank()
                && (profile.getEmail() == null || profile.getEmail().isBlank()
                || isSyntheticEmail(profile.getEmail()))) {
            profile.setEmail(email);
            if (("user-" + userId).equals(profile.getDisplayName())) {
                profile.setDisplayName(null);
            }
            profile.touch();
            profileRepository.save(profile);
            upsertReadModel(profile);
        }
        return profile;
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
        readModel.setEmail(isSyntheticEmail(profile.getEmail()) ? null : profile.getEmail());
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
        String email = isSyntheticEmail(rm.getEmail()) ? null : rm.getEmail();
        return new ProfileResponse(
                rm.getUserId(), email,
                deriveDisplayName(rm.getDisplayName(), email),
                rm.getBio(),
                rm.getLocation(), rm.getAvatarUrl(), rm.getFollowersCount(),
                rm.getFollowingCount(), rm.getPostsCount(), rm.getCreatedAt(), rm.getUpdatedAt()
        );
    }

    private boolean isSyntheticEmail(String email) {
        return email != null && email.endsWith("@booksocial.local");
    }
}
