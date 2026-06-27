package com.smartinventory.config;

import com.smartinventory.entity.User;
import com.smartinventory.enums.ProfileStatus;
import com.smartinventory.enums.Role;
import com.smartinventory.enums.UserStatus;
import com.smartinventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final com.smartinventory.repository.ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_EMAIL    = "admin@smartinventory.com";
    private static final String ADMIN_PASSWORD = "Smart@1234";

    @Override
    public void run(String... args) {
        User admin;
        if (!userRepository.existsByEmail(ADMIN_EMAIL)) {
            admin = User.builder()
                    .email(ADMIN_EMAIL)
                    .password(passwordEncoder.encode(ADMIN_PASSWORD))
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .profileStatus(ProfileStatus.COMPLETED)
                    .emailVerifiedAt(LocalDateTime.now())
                    .lastLoginAt(LocalDateTime.now())
                    .build();
            userRepository.save(admin);
            System.out.println(">>> Admin Created: " + ADMIN_EMAIL + " / " + ADMIN_PASSWORD);
        } else {
            admin = userRepository.findByEmail(ADMIN_EMAIL).get();
        }

        com.smartinventory.entity.Profile profile = profileRepository.findByUserId(admin.getId())
                .orElseGet(() -> com.smartinventory.entity.Profile.builder().user(admin).build());
        profile.setFirstName("System");
        profile.setLastName("Admin");
        profile.setBusinessName("Smart Inventory HQ");
        profile.setBusinessType("Platform");
        profile.setProfileImageUrl("smart_inventory_logo.png");
        profileRepository.save(profile);
    }
}
