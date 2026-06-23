package com.smartinventory.service.impl;

import com.smartinventory.dto.request.LoginRequest;
import com.smartinventory.dto.request.RegisterRequest;
import com.smartinventory.dto.request.VerifyOtpRequest;
import com.smartinventory.dto.response.AuthResponse;
import com.smartinventory.entity.Otp;
import com.smartinventory.entity.User;
import com.smartinventory.enums.OtpType;
import com.smartinventory.enums.ProfileStatus;
import com.smartinventory.enums.Role;
import com.smartinventory.enums.UserStatus;
import com.smartinventory.exception.BadRequestException;
import com.smartinventory.exception.UnauthorizedException;
import com.smartinventory.repository.OtpRepository;
import com.smartinventory.repository.UserRepository;
import com.smartinventory.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository  userRepository;
    private final OtpRepository   otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil         jwtUtil;
    private final EmailService    emailService;

    @Transactional
    public String register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.valueOf(req.getRole().toUpperCase()))
                .status(UserStatus.PENDING)
                .profileStatus(ProfileStatus.INCOMPLETE)
                .build();

        userRepository.save(user);
        generateAndSaveOtp(user, OtpType.EMAIL_VERIFY);

        return "OTP sent to " + req.getEmail() + ". Please verify your email.";
    }

    @Transactional
    public String verifyEmailOtp(VerifyOtpRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        Otp otp = otpRepository
                .findTopByUserIdAndTypeAndIsUsedFalseOrderByCreatedAtDesc(user.getId(), OtpType.EMAIL_VERIFY)
                .orElseThrow(() -> new BadRequestException("No active OTP found"));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP expired");
        }
        if (!otp.getOtp().equals(req.getOtp())) {
            throw new BadRequestException("Invalid OTP");
        }

        otp.setIsUsed(true);
        otpRepository.save(otp);

        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        return "Email verified successfully. Please login.";
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        if (user.getEmailVerifiedAt() == null) {
            throw new UnauthorizedException("Please verify your email first");
        }
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new UnauthorizedException("Account blocked by admin");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .userId(user.getId())
                .profileCompleted(user.getProfileStatus() == ProfileStatus.COMPLETED)
                .status(user.getStatus().name())
                .build();
    }

    @Transactional
    public String resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        generateAndSaveOtp(user, OtpType.EMAIL_VERIFY);
        return "OTP resent to " + email;
    }

    private void generateAndSaveOtp(User user, OtpType type) {
        String code = String.format("%06d", new Random().nextInt(999999));

        Otp otp = Otp.builder()
                .user(user)
                .otp(code)
                .type(type)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();

        otpRepository.save(otp);

        // Send email (async - won't block registration)
        emailService.sendOtpEmail(user.getEmail(), code, "Email Verification");

        // Also print to console for dev convenience
        System.out.println(">>> OTP for " + user.getEmail() + ": " + code);
    }
}
