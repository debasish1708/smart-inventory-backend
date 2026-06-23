package com.smartinventory.controller;

import com.smartinventory.dto.request.UpdateProfileRequest;
import com.smartinventory.dto.response.ApiResponse;
import com.smartinventory.dto.response.ProfileResponse;
import com.smartinventory.service.impl.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<ProfileResponse>> completeProfile(
            Authentication auth, @Valid @RequestBody UpdateProfileRequest req) {
        ProfileResponse resp = profileService.saveProfile(auth.getName(), req);
        return ResponseEntity.ok(ApiResponse.ok("Profile saved. Awaiting admin approval.", resp));
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            Authentication auth, @Valid @RequestBody UpdateProfileRequest req) {
        ProfileResponse resp = profileService.saveProfile(auth.getName(), req);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", resp));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Profile fetched", profileService.getMyProfile(auth.getName())));
    }
}
