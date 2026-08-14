package com.booksocial.user.web;

import com.booksocial.user.service.ProfileService;
import com.booksocial.user.web.dto.ProfileResponse;
import com.booksocial.user.web.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ProfileResponse me(@RequestHeader("X-User-Id") Long userId,
                              @RequestHeader("X-User-Email") String email) {
        return profileService.getOrCreate(userId, email);
    }

    @PutMapping("/me")
    public ProfileResponse updateMe(@RequestHeader("X-User-Id") Long userId,
                                    @RequestHeader("X-User-Email") String email,
                                    @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.update(userId, email, request);
    }

    @GetMapping("/{userId}")
    public ProfileResponse byUserId(@PathVariable("userId") Long userId) {
        return profileService.getByUserId(userId);
    }

}
