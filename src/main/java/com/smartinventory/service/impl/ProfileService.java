package com.smartinventory.service.impl;

import com.smartinventory.dto.request.UpdateProfileRequest;
import com.smartinventory.dto.response.ProfileResponse;
import com.smartinventory.entity.Profile;
import com.smartinventory.entity.User;
import com.smartinventory.enums.ProfileStatus;
import com.smartinventory.exception.BadRequestException;
import com.smartinventory.repository.ProfileRepository;
import com.smartinventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository    userRepository;
    private final ProfileRepository profileRepository;

    @Transactional
    public ProfileResponse saveProfile(String email, UpdateProfileRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> Profile.builder().user(user).build());

        profile.setFirstName(req.getFirstName());
        profile.setLastName(req.getLastName());
        profile.setBusinessName(req.getBusinessName());
        profile.setBusinessType(req.getBusinessType());
        profile.setMobNo(req.getMobNo());
        profile.setAddress(req.getAddress());
        profile.setGst(req.getGst());
        profile.setCity(req.getCity());
        profile.setState(req.getState());
        profile.setPincode(req.getPincode());
        profile.setProfileImageUrl(req.getProfileImageUrl());
        profileRepository.save(profile);

        if (user.getProfileStatus() != ProfileStatus.COMPLETED) {
            user.setProfileStatus(ProfileStatus.COMPLETED);
            userRepository.save(user);
        }

        return toResponse(user, profile);
    }

    @Transactional
    public ProfileResponse uploadProfileImage(String email, org.springframework.web.multipart.MultipartFile file) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> Profile.builder().user(user).build());

        String subFolder = user.getRole() == com.smartinventory.enums.Role.RETAILER ? "retailers" : "suppliers";
        String baseDir = "images/profile/" + subFolder;
        java.io.File directory = new java.io.File(System.getProperty("user.dir"), baseDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = user.getId() + "_" + System.currentTimeMillis() + ext;
        java.io.File destFile = new java.io.File(directory, uniqueFilename);
        try {
            file.transferTo(destFile);
        } catch (Exception e) {
            throw new BadRequestException("Failed to save image file: " + e.getMessage());
        }

        profile.setProfileImageUrl(uniqueFilename);
        profileRepository.save(profile);

        return toResponse(user, profile);
    }

    public ProfileResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        return toResponse(user, profile);
    }

    private ProfileResponse toResponse(User user, Profile profile) {
        ProfileResponse.ProfileResponseBuilder builder = ProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .userStatus(user.getStatus().name())
                .profileCompleted(user.getProfileStatus() == ProfileStatus.COMPLETED);

        if (profile != null) {
            builder
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .businessName(profile.getBusinessName())
                .businessType(profile.getBusinessType())
                .mobNo(profile.getMobNo())
                .address(profile.getAddress())
                .gst(profile.getGst())
                .city(profile.getCity())
                .state(profile.getState())
                .pincode(profile.getPincode())
                .profileImageUrl(profile.getProfileImageUrl());
        }

        return builder.build();
    }
}
