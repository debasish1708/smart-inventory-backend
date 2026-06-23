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
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/supplier")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPPLIER')")
public class SupplierController {

    private final UserRepository              userRepo;
    private final SupplierInventoryRepository invRepo;
    private final OrderRepository             orderRepo;
    private final OrderItemRepository         orderItemRepo;
    private final RatingRepository            ratingRepo;
    private final SubscriptionRepository      subRepo;
    private final ProductRepository           productRepo;
    private final CategoryRepository          categoryRepo;

    private User getUser(Authentication auth) {
        return userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new BadRequestException("User not found"));
    }

    // ── INVENTORY ──────────────────────────────────────────────────────────

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getInventory(Authentication auth) {
        User user = getUser(auth);
        List<InventoryResponse> list = invRepo.findByUserId(user.getId())
            .stream().map(this::toInvResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Inventory", list));
    }

    @PostMapping("/inventory")
    public ResponseEntity<ApiResponse<InventoryResponse>> addInventory(
            Authentication auth, @RequestBody InventoryRequest req) {
        User user = getUser(auth);
        Product product = resolveOrCreateProduct(req);
        SupplierInventory item = new SupplierInventory();
        item.setUser(user);
        item.setProduct(product);
        item.setPrice(req.getPrice() != null ? req.getPrice() : BigDecimal.ZERO);
        item.setMoq(req.getMoq() != null ? req.getMoq() : 1);
        item.setStockQuantity(req.getStockQuantity() != null ? req.getStockQuantity() : 0);
        item.setLeadTime(req.getLeadTime() != null ? req.getLeadTime() : 1);
        item.setIsActive(req.getIsActive() == null || req.getIsActive());
        invRepo.save(item);
        return ResponseEntity.ok(ApiResponse.ok("Listing added", toInvResponse(item)));
    }

    @PutMapping("/inventory/{id}")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateInventory(
            @PathVariable Long id, @RequestBody InventoryRequest req) {
        SupplierInventory item = invRepo.findById(id)
                .orElseThrow(() -> new BadRequestException("Item not found"));
        if (req.getProductName() != null) item.setProduct(resolveOrCreateProduct(req));
        if (req.getPrice()         != null) item.setPrice(req.getPrice());
        if (req.getMoq()           != null) item.setMoq(req.getMoq());
        if (req.getStockQuantity() != null) item.setStockQuantity(req.getStockQuantity());
        if (req.getLeadTime()      != null) item.setLeadTime(req.getLeadTime());
        if (req.getIsActive()      != null) item.setIsActive(req.getIsActive());
        invRepo.save(item);
        return ResponseEntity.ok(ApiResponse.ok("Updated", toInvResponse(item)));
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
        List<OrderResponse> list = orderRepo.findBySupplierIdOrderByOrderDateDesc(user.getId())
            .stream().map(this::toOrderResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Orders", list));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<String>> updateOrderStatus(
            @PathVariable Long id, @RequestParam String status) {
        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new BadRequestException("Order not found"));
        List<String> allowed = List.of("ACCEPTED", "DISPATCHED", "DELIVERED", "CANCELLED");
        if (!allowed.contains(status.toUpperCase()))
            throw new BadRequestException("Invalid status: " + status);
        order.setStatus(status.toUpperCase());
        if ("DELIVERED".equals(status.toUpperCase()))
            order.setDeliveredDate(LocalDateTime.now());
        orderRepo.save(order);
        return ResponseEntity.ok(ApiResponse.ok("Order status updated to " + status));
    }

    // ── RATINGS ────────────────────────────────────────────────────────────

    @GetMapping("/ratings")
    public ResponseEntity<ApiResponse<List<RatingResponse>>> getRatings(Authentication auth) {
        User user = getUser(auth);
        List<RatingResponse> list = ratingRepo.findBySupplierId(user.getId()).stream()
            .map(r -> RatingResponse.builder()
                .id(r.getId()).rating(r.getRating()).review(r.getReview())
                .retailerEmail(r.getRetailer() != null ? r.getRetailer().getEmail() : null)
                .createdAt(r.getCreatedAt()).build())
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Ratings", list));
    }

    // ── ANALYTICS ──────────────────────────────────────────────────────────

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalytics(Authentication auth) {
        User user = getUser(auth);
        long totalOrders = orderRepo.countBySupplierId(user.getId());
        BigDecimal totalRevenue = orderRepo.sumRevenueForSupplier(user.getId());
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        Double avgRating = ratingRepo.avgRatingForSupplier(user.getId());

        String topProduct = orderRepo.findBySupplierIdOrderByOrderDateDesc(user.getId()).stream()
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
            .topProduct(topProduct).monthlyGrowth(avgRating != null ? avgRating : 0.0).build()));
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

    // ── REPORT ─────────────────────────────────────────────────────────────

    @GetMapping("/report")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getReport(Authentication auth) {
        User user = getUser(auth);
        // Returns all delivered orders as the supplier's report
        List<OrderResponse> list = orderRepo.findBySupplierIdOrderByOrderDateDesc(user.getId())
            .stream()
            .filter(o -> "DELIVERED".equals(o.getStatus()))
            .map(this::toOrderResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Report", list));
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

    private InventoryResponse toInvResponse(SupplierInventory i) {
        String productName = i.getProduct() != null ? i.getProduct().getName()  : "Unknown";
        String category    = i.getProduct() != null && i.getProduct().getCategory() != null
                            ? i.getProduct().getCategory().getName() : "";
        String brand       = i.getProduct() != null ? i.getProduct().getBrand() : null;
        String unit        = i.getProduct() != null ? i.getProduct().getUnit()  : null;
        String sku         = i.getProduct() != null ? i.getProduct().getSku()   : null;
        return InventoryResponse.builder()
            .id(i.getId()).productName(productName).category(category)
            .brand(brand).unit(unit).sku(sku).price(i.getPrice())
            .moq(i.getMoq()).stockQuantity(i.getStockQuantity())
            .leadTime(i.getLeadTime()).isActive(i.getIsActive()).build();
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
}
