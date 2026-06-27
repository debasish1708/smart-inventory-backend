package com.smartinventory.controller;

import com.smartinventory.dto.response.ApiResponse;
import com.smartinventory.dto.response.ProfileResponse;
import com.smartinventory.entity.Profile;
import com.smartinventory.entity.User;
import com.smartinventory.enums.UserStatus;
import com.smartinventory.exception.BadRequestException;
import com.smartinventory.repository.ProfileRepository;
import com.smartinventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final ProfileRepository profileRepository;
    private final UserRepository    userRepository;

    @GetMapping("/pending-users")
    public ResponseEntity<ApiResponse<List<User>>> getPendingUsers() {
        return ResponseEntity.ok(ApiResponse.ok("Pending users",
                userRepository.findByStatus(UserStatus.PENDING)));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Profile>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok("All profiles", profileRepository.findAll()));
    }

    @GetMapping("/all-users-raw")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsersRaw() {
        return ResponseEntity.ok(ApiResponse.ok("All users", userRepository.findAll()));
    }

    @PutMapping("/approve/{userId}")
    public ResponseEntity<ApiResponse<String>> approveUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setStatus(UserStatus.ACTIVE);
        user.setAdminVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("User approved: " + user.getEmail()));
    }

    @PutMapping("/block/{userId}")
    public ResponseEntity<ApiResponse<String>> blockUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("User blocked: " + user.getEmail()));
    }

    @PutMapping("/unblock/{userId}")
    public ResponseEntity<ApiResponse<String>> unblockUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("User unblocked: " + user.getEmail()));
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        userRepository.delete(user);
        return ResponseEntity.ok(ApiResponse.ok("User deleted: " + user.getEmail()));
    }

    @GetMapping("/user-profile/{userId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getUserProfile(@PathVariable Long userId) {
        Profile profile = profileRepository.findByUserId(userId).orElse(null);
        User user = userRepository.findById(userId).orElseThrow(() -> new BadRequestException("User not found"));
        ProfileResponse.ProfileResponseBuilder builder = ProfileResponse.builder()
                .userId(user.getId()).email(user.getEmail()).role(user.getRole().name())
                .userStatus(user.getStatus().name())
                .profileCompleted(user.getProfileStatus() == com.smartinventory.enums.ProfileStatus.COMPLETED);
        if (profile != null) {
            builder.firstName(profile.getFirstName()).lastName(profile.getLastName())
                .businessName(profile.getBusinessName()).businessType(profile.getBusinessType())
                .mobNo(profile.getMobNo()).address(profile.getAddress()).gst(profile.getGst())
                .city(profile.getCity()).state(profile.getState()).pincode(profile.getPincode())
                .profileImageUrl(profile.getProfileImageUrl());
        }
        return ResponseEntity.ok(ApiResponse.ok("Profile fetched", builder.build()));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStats() {
        long total   = userRepository.count();
        long pending = userRepository.findByStatus(UserStatus.PENDING).size();
        long active  = userRepository.findByStatus(UserStatus.ACTIVE).size();
        long blocked = userRepository.findByStatus(UserStatus.BLOCKED).size();
        return ResponseEntity.ok(ApiResponse.ok("Stats", Map.of(
                "total", total,
                "pending", pending,
                "active", active,
                "blocked", blocked
        )));
    }
}
