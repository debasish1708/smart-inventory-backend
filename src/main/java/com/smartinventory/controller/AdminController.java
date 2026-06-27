package com.smartinventory.controller;

import com.smartinventory.dto.response.AdminAnalyticsResponse;
import com.smartinventory.dto.response.ApiResponse;
import com.smartinventory.dto.response.ProfileResponse;
import com.smartinventory.entity.*;
import com.smartinventory.enums.UserStatus;
import com.smartinventory.exception.BadRequestException;
import com.smartinventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final ProfileRepository      profileRepository;
    private final UserRepository         userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository      paymentRepository;
    private final RetailerSaleRepository retailerSaleRepository;
    private final OrderRepository        orderRepository;
    private final OrderItemRepository    orderItemRepository;

    @lombok.Data @lombok.Builder
    public static class AdminUserResponse {
        private Long id;
        private String email;
        private String role;
        private String status;
        private String profileStatus;
        private LocalDateTime createdAt;
        private String profileImageUrl;
    }

    @GetMapping("/pending-users")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getPendingUsers() {
        List<AdminUserResponse> list = userRepository.findByStatus(UserStatus.PENDING).stream().map(user -> {
            String imgUrl = profileRepository.findByUserId(user.getId())
                    .map(Profile::getProfileImageUrl)
                    .orElse(null);
            return AdminUserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .status(user.getStatus().name())
                    .profileStatus(user.getProfileStatus().name())
                    .createdAt(user.getCreatedAt())
                    .profileImageUrl(imgUrl)
                    .build();
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Pending users", list));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Profile>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok("All profiles", profileRepository.findAll()));
    }

    @GetMapping("/all-users-raw")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getAllUsersRaw() {
        List<AdminUserResponse> list = userRepository.findAll().stream().map(user -> {
            String imgUrl = profileRepository.findByUserId(user.getId())
                    .map(Profile::getProfileImageUrl)
                    .orElse(null);
            return AdminUserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .status(user.getStatus().name())
                    .profileStatus(user.getProfileStatus().name())
                    .createdAt(user.getCreatedAt())
                    .profileImageUrl(imgUrl)
                    .build();
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("All users", list));
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

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<AdminAnalyticsResponse>> getAnalytics() {
        long totalUsers   = userRepository.count();
        long pendingUsers = userRepository.findByStatus(UserStatus.PENDING).size();
        long activeUsers  = userRepository.findByStatus(UserStatus.ACTIVE).size();
        long blockedUsers = userRepository.findByStatus(UserStatus.BLOCKED).size();

        long subscriptionCount = subscriptionRepository.count();
        BigDecimal subscriptionRevenue = paymentRepository.findAll().stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long retailerSalesCount = retailerSaleRepository.count();
        BigDecimal retailerSalesRevenue = retailerSaleRepository.findAll().stream()
                .map(RetailerSale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long supplierSalesCount = orderRepository.count();
        BigDecimal supplierSalesRevenue = orderItemRepository.findAll().stream()
                .filter(oi -> oi.getOrder() != null && !"CANCELLED".equalsIgnoreCase(oi.getOrder().getStatus()))
                .map(oi -> oi.getPrice().multiply(BigDecimal.valueOf(oi.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM");
        
        List<AdminAnalyticsResponse.MonthlyRevenue> monthlyRetailerRevenue = new ArrayList<>();
        List<AdminAnalyticsResponse.MonthlyRevenue> monthlySupplierRevenue = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();
        List<RetailerSale> retailerSalesList = retailerSaleRepository.findAll();
        List<OrderItem> orderItemsList = orderItemRepository.findAll().stream()
                .filter(oi -> oi.getOrder() != null && !"CANCELLED".equalsIgnoreCase(oi.getOrder().getStatus()))
                .collect(Collectors.toList());

        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);
            String monthName = monthStart.format(formatter);

            BigDecimal retSales = retailerSalesList.stream()
                    .filter(s -> s.getSaleDate().isAfter(monthStart) && s.getSaleDate().isBefore(monthEnd))
                    .map(RetailerSale::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal supSales = orderItemsList.stream()
                    .filter(oi -> oi.getOrder().getOrderDate().isAfter(monthStart) && oi.getOrder().getOrderDate().isBefore(monthEnd))
                    .map(oi -> oi.getPrice().multiply(BigDecimal.valueOf(oi.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            monthlyRetailerRevenue.add(new AdminAnalyticsResponse.MonthlyRevenue(monthName, retSales));
            monthlySupplierRevenue.add(new AdminAnalyticsResponse.MonthlyRevenue(monthName, supSales));
        }

        AdminAnalyticsResponse response = AdminAnalyticsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .pendingUsers(pendingUsers)
                .blockedUsers(blockedUsers)
                .subscriptionCount(subscriptionCount)
                .subscriptionRevenue(subscriptionRevenue)
                .retailerSalesCount(retailerSalesCount)
                .retailerSalesRevenue(retailerSalesRevenue)
                .supplierSalesCount(supplierSalesCount)
                .supplierSalesRevenue(supplierSalesRevenue)
                .monthlyRetailerRevenue(monthlyRetailerRevenue)
                .monthlySupplierRevenue(monthlySupplierRevenue)
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Admin Analytics Fetched Successfully", response));
    }
}
