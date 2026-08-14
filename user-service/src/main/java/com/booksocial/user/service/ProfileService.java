package com.booksocial.user.service;

import com.booksocial.user.domain.Profile;
import com.booksocial.user.domain.ProfileNotFoundException;
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
        Profile profile = profileRepository.findByUserId(userId).orElseGet(() -> {
            Profile created = new Profile();
            created.setUserId(userId);
            created.setEmail(email);
            return profileRepository.save(created);
        });
        return toResponse(upsertReadModel(profile));
    }

    public ProfileResponse update(Long userId, String email, UpdateProfileRequest request) {
        Profile profile = profileRepository.findByUserId(userId).orElseGet(() -> {
            Profile created = new Profile();
            created.setUserId(userId);
            created.setEmail(email);
            return profileRepository.save(created);
        });
        if (request.displayName() != null) profile.setDisplayName(request.displayName());
        if (request.bio() != null) profile.setBio(request.bio());
        if (request.location() != null) profile.setLocation(request.location());
        if (request.avatarUrl() != null) profile.setAvatarUrl(request.avatarUrl());
        profile.touch();
        profileRepository.save(profile);
        return toResponse(upsertReadModel(profile));
    }

    public ProfileResponse getByUserId(Long userId) {
        ProfileReadModel readModel = readModelRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException(userId));
        return toResponse(readModel);
    }

    private ProfileReadModel upsertReadModel(Profile profile) {
        ProfileReadModel readModel = readModelRepository.findByUserId(profile.getUserId())
                .orElseGet(() -> new ProfileReadModel(profile.getUserId(), profile.getEmail()));
        readModel.setUserId(profile.getUserId());
        readModel.setEmail(profile.getEmail());
        readModel.setDisplayName(profile.getDisplayName());
        readModel.setBio(profile.getBio());
        readModel.setLocation(profile.getLocation());
        readModel.setAvatarUrl(profile.getAvatarUrl());
        readModel.setUpdatedAt(profile.getUpdatedAt());
        return readModelRepository.save(readModel);
    }

    private ProfileResponse toResponse(ProfileReadModel rm) {
        return new ProfileResponse(
                rm.getUserId(), rm.getEmail(), rm.getDisplayName(), rm.getBio(),
                rm.getLocation(), rm.getAvatarUrl(), rm.getFollowersCount(),
                rm.getFollowingCount(), rm.getPostsCount(), rm.getCreatedAt(), rm.getUpdatedAt()
        );
    }
}
