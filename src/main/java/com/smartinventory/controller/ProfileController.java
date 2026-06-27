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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
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

    @PostMapping("/upload-image")
    public ResponseEntity<ApiResponse<ProfileResponse>> uploadImage(
            Authentication auth, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        ProfileResponse resp = profileService.uploadProfileImage(auth.getName(), file);
        return ResponseEntity.ok(ApiResponse.ok("Profile image uploaded successfully", resp));
    }

    @GetMapping("/image/{role}/{filename}")
    public ResponseEntity<org.springframework.core.io.Resource> getProfileImage(
            @PathVariable("role") String role, @PathVariable("filename") String filename) {
        try {
            String subFolder = "retailers";
            if ("suppliers".equalsIgnoreCase(role) || "supplier".equalsIgnoreCase(role)) {
                subFolder = "suppliers";
            }
            java.nio.file.Path filePath = java.nio.file.Paths.get(System.getProperty("user.dir"), "images/profile/" + subFolder).resolve(filename).normalize();
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());
            if (resource.exists()) {
                String contentType = "image/jpeg";
                if (filename.toLowerCase().endsWith(".png")) contentType = "image/png";
                else if (filename.toLowerCase().endsWith(".gif")) contentType = "image/gif";
                else if (filename.toLowerCase().endsWith(".webp")) contentType = "image/webp";
                
                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
