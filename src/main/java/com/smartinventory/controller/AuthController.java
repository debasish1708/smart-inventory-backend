package com.smartinventory.controller;

import com.smartinventory.dto.request.LoginRequest;
import com.smartinventory.dto.request.RegisterRequest;
import com.smartinventory.dto.request.VerifyOtpRequest;
import com.smartinventory.dto.response.ApiResponse;
import com.smartinventory.dto.response.AuthResponse;
import com.smartinventory.enums.OtpType;
import com.smartinventory.repository.OtpRepository;
import com.smartinventory.repository.UserRepository;
import com.smartinventory.service.impl.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService    authService;
    private final OtpRepository  otpRepository;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Registration successful", authService.register(req)));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.verifyEmailOtp(req)));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOtp(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.ok(authService.resendOtp(email)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authService.login(req)));
    }

    /** Dev-only endpoint to retrieve OTP without checking email */
    @GetMapping("/otp-debug")
    public ResponseEntity<ApiResponse<String>> debugOtp(@RequestParam String email) {
        return userRepository.findByEmail(email)
                .flatMap(user -> otpRepository
                        .findTopByUserIdAndTypeAndIsUsedFalseOrderByCreatedAtDesc(user.getId(), OtpType.EMAIL_VERIFY))
                .map(otp -> ResponseEntity.ok(ApiResponse.ok("OTP (dev only)", otp.getOtp())))
                .orElse(ResponseEntity.ok(ApiResponse.fail("No active OTP found")));
    }
}
