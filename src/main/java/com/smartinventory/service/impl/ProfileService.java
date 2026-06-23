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
