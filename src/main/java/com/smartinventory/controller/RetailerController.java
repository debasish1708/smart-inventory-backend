package com.smartinventory.controller;

import com.smartinventory.dto.request.InventoryRequest;
import com.smartinventory.dto.response.*;
import com.smartinventory.entity.*;
import com.smartinventory.exception.BadRequestException;
import com.smartinventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/retailer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RETAILER')")
public class RetailerController {

    private final UserRepository              userRepo;
    private final RetailerInventoryRepository invRepo;
    private final OrderRepository             orderRepo;
    private final OrderItemRepository         orderItemRepo;
    private final RatingRepository            ratingRepo;
    private final NotificationRepository      notifRepo;
    private final SubscriptionRepository      subRepo;
    private final SupplierInventoryRepository supplierInvRepo;
    private final ProductRepository           productRepo;
    private final CategoryRepository          categoryRepo;
    private final ProfileRepository           profileRepo;

    private User getUser(Authentication auth) {
        return userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new BadRequestException("User not found"));
    }

    // ── INVENTORY ──────────────────────────────────────────────────────────

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getInventory(Authentication auth) {
        User user = getUser(auth);
        List<InventoryResponse> list = invRepo.findAll().stream()
            .filter(i -> i.getUser() != null && i.getUser().getId().equals(user.getId()))
            .map(this::toInventoryResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Inventory", list));
    }

    @PostMapping("/inventory")
    public ResponseEntity<ApiResponse<InventoryResponse>> addInventory(
            Authentication auth, @RequestBody InventoryRequest req) {
        User user = getUser(auth);
        Product product = resolveOrCreateProduct(req);
        RetailerInventory item = new RetailerInventory();
        item.setUser(user);
        item.setProduct(product);
        item.setQuantity(req.getQuantity() != null ? req.getQuantity() : 0);
        item.setThresholdValue(req.getThresholdValue() != null ? req.getThresholdValue() : 10);
        item.setPrice(req.getPrice() != null ? req.getPrice() : BigDecimal.ZERO);
        invRepo.save(item);
        return ResponseEntity.ok(ApiResponse.ok("Item added", toInventoryResponse(item)));
    }

    @PutMapping("/inventory/{id}")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateInventory(
            @PathVariable Long id, @RequestBody InventoryRequest req) {
        RetailerInventory item = invRepo.findById(id)
                .orElseThrow(() -> new BadRequestException("Item not found"));
        if (req.getProductName() != null) {
            Product product = resolveOrCreateProduct(req);
            item.setProduct(product);
        }
        if (req.getQuantity()       != null) item.setQuantity(req.getQuantity());
        if (req.getThresholdValue() != null) item.setThresholdValue(req.getThresholdValue());
        if (req.getPrice()          != null) item.setPrice(req.getPrice());
        invRepo.save(item);
        return ResponseEntity.ok(ApiResponse.ok("Updated", toInventoryResponse(item)));
    }

    @DeleteMapping("/inventory/{id}")
    public ResponseEntity<ApiResponse<String>> deleteInventory(@PathVariable Long id) {
        invRepo.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("Deleted"));
    }

    // ── ORDERS ─────────────────────────────────────────────────────────────

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(Authentication auth) {
        User user = getUser(auth);
        List<OrderResponse> list = orderRepo.findByRetailerIdOrderByOrderDateDesc(user.getId())
            .stream().map(this::toOrderResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Orders", list));
    }

    // ── SUPPLIER MATCH ─────────────────────────────────────────────────────

    @GetMapping("/supplier-match/all")
    public ResponseEntity<ApiResponse<List<SupplierMatchResponse>>> getAllMatches() {
        List<SupplierMatchResponse> list = supplierInvRepo.findAllActive()
            .stream().map(this::toMatchResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Matches", list));
    }

    @GetMapping("/supplier-match")
    public ResponseEntity<ApiResponse<List<SupplierMatchResponse>>> searchByProduct(
            @RequestParam("productId") Long productId) {
        List<SupplierMatchResponse> list = supplierInvRepo.findByProductId(productId)
            .stream().map(this::toMatchResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Matches", list));
    }

    // ── RATINGS ────────────────────────────────────────────────────────────

    @GetMapping("/ratings")
    public ResponseEntity<ApiResponse<List<RatingResponse>>> getRatings(Authentication auth) {
        User user = getUser(auth);
        List<RatingResponse> list = ratingRepo.findByRetailerId(user.getId())
            .stream().map(r -> RatingResponse.builder()
                .id(r.getId()).rating(r.getRating()).review(r.getReview())
                .supplierEmail(r.getSupplier() != null ? r.getSupplier().getEmail() : null)
                .createdAt(r.getCreatedAt()).build())
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Ratings", list));
    }

    // ── NOTIFICATIONS ──────────────────────────────────────────────────────

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<NotificationRetailer>>> getNotifications(Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(ApiResponse.ok("Notifications",
            notifRepo.findByRetailerIdOrderByCreatedAtDesc(user.getId())));
    }

    // ── SUBSCRIPTION ───────────────────────────────────────────────────────

    @GetMapping("/subscription")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscription(Authentication auth) {
        User user = getUser(auth);
        return subRepo.findTopByUserIdOrderByStartDateTimeDesc(user.getId())
            .map(s -> ResponseEntity.ok(ApiResponse.ok("Subscription", SubscriptionResponse.builder()
                .id(s.getId()).planName(s.getPlanName().name()).status(s.getStatus())
                .startDateTime(s.getStartDateTime()).endDateTime(s.getEndDateTime()).build())))
            .orElse(ResponseEntity.ok(ApiResponse.ok("No subscription", null)));
    }

    // ── ANALYTICS ──────────────────────────────────────────────────────────

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalytics(Authentication auth) {
        User user = getUser(auth);
        long totalOrders = orderRepo.countByRetailerId(user.getId());
        BigDecimal totalRevenue = orderRepo.sumRevenueForRetailer(user.getId());
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        // top product: most ordered
        String topProduct = orderRepo.findByRetailerIdOrderByOrderDateDesc(user.getId()).stream()
            .flatMap(o -> orderItemRepo.findByOrderId(o.getId()).stream())
            .collect(Collectors.groupingBy(
                oi -> oi.getProduct() != null ? oi.getProduct().getName() : "Unknown",
                Collectors.summingInt(oi -> oi.getQuantity() != null ? oi.getQuantity() : 0)))
            .entrySet().stream()
            .max(java.util.Map.Entry.comparingByValue())
            .map(java.util.Map.Entry::getKey)
            .orElse("—");

        return ResponseEntity.ok(ApiResponse.ok("Analytics", AnalyticsResponse.builder()
            .totalOrders(totalOrders).totalRevenue(totalRevenue)
            .topProduct(topProduct).monthlyGrowth(0).build()));
    }

    // ── HELPERS ────────────────────────────────────────────────────────────

    private Product resolveOrCreateProduct(InventoryRequest req) {
        if (req.getProductName() == null || req.getProductName().isBlank())
            throw new BadRequestException("Product name is required");

        return productRepo.findByNameIgnoreCase(req.getProductName()).orElseGet(() -> {
            Category cat = categoryRepo.findByNameIgnoreCase(
                req.getCategory() != null ? req.getCategory() : "General")
                .orElseGet(() -> {
                    Category c = new Category();
                    c.setName(req.getCategory() != null ? req.getCategory() : "General");
                    return categoryRepo.save(c);
                });
            Product p = new Product();
            p.setName(req.getProductName());
            p.setCategory(cat);
            p.setBrand(req.getBrand());
            p.setUnit(req.getUnit());
            p.setSku(req.getSku());
            return productRepo.save(p);
        });
    }

    private InventoryResponse toInventoryResponse(RetailerInventory i) {
        String productName = i.getProduct() != null ? i.getProduct().getName() : "Unknown";
        String category    = i.getProduct() != null && i.getProduct().getCategory() != null
                            ? i.getProduct().getCategory().getName() : "";
        String brand       = i.getProduct() != null ? i.getProduct().getBrand() : null;
        String unit        = i.getProduct() != null ? i.getProduct().getUnit()  : null;
        String sku         = i.getProduct() != null ? i.getProduct().getSku()   : null;
        return InventoryResponse.builder()
            .id(i.getId()).productName(productName).category(category)
            .brand(brand).unit(unit).sku(sku).price(i.getPrice())
            .quantity(i.getQuantity()).thresholdValue(i.getThresholdValue())
            .build();
    }

    private OrderResponse toOrderResponse(Order o) {
        List<OrderItemResponse> items = orderItemRepo.findByOrderId(o.getId()).stream()
            .map(oi -> OrderItemResponse.builder()
                .id(oi.getId())
                .productName(oi.getProduct() != null ? oi.getProduct().getName() : "—")
                .quantity(oi.getQuantity()).price(oi.getPrice()).unit(oi.getUnit()).build())
            .collect(Collectors.toList());
        BigDecimal total = items.stream()
            .map(i -> i.getPrice() != null && i.getQuantity() != null
                ? i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())) : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return OrderResponse.builder()
            .id(o.getId()).status(o.getStatus())
            .orderDate(o.getOrderDate()).deliveredDate(o.getDeliveredDate())
            .supplierEmail(o.getSupplier() != null ? o.getSupplier().getEmail() : null)
            .retailerEmail(o.getRetailer() != null ? o.getRetailer().getEmail() : null)
            .items(items).totalAmount(total).build();
    }

    private SupplierMatchResponse toMatchResponse(SupplierInventory si) {
        User supplier = si.getUser();
        Profile profile = supplier != null ? profileRepo.findByUserId(supplier.getId()).orElse(null) : null;
        Double avgRating = supplier != null ? ratingRepo.avgRatingForSupplier(supplier.getId()) : 0.0;
        return SupplierMatchResponse.builder()
            .supplierId(supplier != null ? supplier.getId() : null)
            .supplierName(profile != null ? profile.getFirstName() + " " + profile.getLastName() : (supplier != null ? supplier.getEmail() : "—"))
            .businessName(profile != null ? profile.getBusinessName() : "—")
            .productName(si.getProduct() != null ? si.getProduct().getName() : "—")
            .category(si.getProduct() != null && si.getProduct().getCategory() != null ? si.getProduct().getCategory().getName() : "—")
            .price(si.getPrice()).moq(si.getMoq())
            .stockQuantity(si.getStockQuantity()).leadTime(si.getLeadTime())
            .rating(avgRating != null ? avgRating : 0.0).build();
    }
}
