package com.smartinventory.controller;

import com.smartinventory.dto.request.InventoryRequest;
import com.smartinventory.dto.request.OrderRequest;
import com.smartinventory.dto.request.SaleRequest;
import com.smartinventory.dto.response.*;
import com.smartinventory.entity.*;
import com.smartinventory.enums.SubscriptionPlan;
import com.smartinventory.exception.BadRequestException;
import com.smartinventory.repository.*;
import com.smartinventory.scheduler.LowStockScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/retailer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RETAILER')")
@SecurityRequirement(name = "bearerAuth")
public class RetailerController {

    private final UserRepository              userRepo;
    private final RetailerInventoryRepository invRepo;
    private final OrderRepository             orderRepo;
    private final OrderItemRepository         orderItemRepo;
    private final RatingRepository            ratingRepo;
    private final NotificationRepository      notifRepo;
    private final SubscriptionRepository      subRepo;
    private final PaymentRepository           paymentRepo;
    private final SupplierInventoryRepository supplierInvRepo;
    private final ProductRepository           productRepo;
    private final CategoryRepository          categoryRepo;
    private final ProfileRepository           profileRepo;
    private final RetailerSaleRepository      saleRepo;
    private final RetailerSaleItemRepository  saleItemRepo;
    private final RetailerSalesAnalyticsRepository salesAnalyticsRepo;
    private final LowStockScheduler           lowStockScheduler;

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
            .id(i.getId())
            .productId(i.getProduct() != null ? i.getProduct().getId() : null)
            .productName(productName).category(category)
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
            .productId(si.getProduct() != null ? si.getProduct().getId() : null)
            .supplierId(supplier != null ? supplier.getId() : null)
            .supplierName(profile != null ? profile.getFirstName() + " " + profile.getLastName() : (supplier != null ? supplier.getEmail() : "—"))
            .businessName(profile != null ? profile.getBusinessName() : "—")
            .productName(si.getProduct() != null ? si.getProduct().getName() : "—")
            .category(si.getProduct() != null && si.getProduct().getCategory() != null ? si.getProduct().getCategory().getName() : "—")
            .price(si.getPrice()).moq(si.getMoq())
            .stockQuantity(si.getStockQuantity()).leadTime(si.getLeadTime())
            .rating(avgRating != null ? avgRating : 0.0).build();
    }

    // ── SALES & TRANSACTIONS ───────────────────────────────────────────────

    @PostMapping("/sales")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<SaleResponse>> checkoutSale(
            Authentication auth, @RequestBody SaleRequest req) {
        User user = getUser(auth);

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<RetailerSaleItem> saleItems = new ArrayList<>();
        List<RetailerInventory> updatedInventories = new ArrayList<>();
        List<RetailerSalesAnalytics> analyticsLogs = new ArrayList<>();

        for (SaleRequest.SaleItemRequest itemReq : req.getItems()) {
            Product product = productRepo.findById(itemReq.getProductId())
                    .orElseThrow(() -> new BadRequestException("Product not found: " + itemReq.getProductId()));

            RetailerInventory invItem = invRepo.findAll().stream()
                    .filter(i -> i.getUser() != null && i.getUser().getId().equals(user.getId()) 
                              && i.getProduct() != null && i.getProduct().getId().equals(product.getId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Product '" + product.getName() + "' is not configured in your inventory."));

            if (invItem.getQuantity() < itemReq.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product '" + product.getName() + "'. Available: " + invItem.getQuantity() + ", requested: " + itemReq.getQuantity());
            }

            invItem.setQuantity(invItem.getQuantity() - itemReq.getQuantity());
            updatedInventories.add(invItem);

            BigDecimal itemPrice = invItem.getPrice() != null ? invItem.getPrice() : BigDecimal.ZERO;
            BigDecimal itemTotal = itemPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            RetailerSaleItem saleItem = RetailerSaleItem.builder()
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .price(itemPrice)
                    .unit(product.getUnit())
                    .build();
            saleItems.add(saleItem);

            RetailerSalesAnalytics analytics = RetailerSalesAnalytics.builder()
                    .product(product)
                    .user(user)
                    .date(LocalDate.now())
                    .quantity(itemReq.getQuantity())
                    .price(itemPrice)
                    .totalValue(itemTotal)
                    .build();
            analyticsLogs.add(analytics);
        }

        RetailerSale sale = RetailerSale.builder()
                .retailer(user)
                .totalAmount(totalAmount)
                .saleDate(LocalDateTime.now())
                .build();

        for (RetailerSaleItem saleItem : saleItems) {
            saleItem.setRetailerSale(sale);
        }
        sale.setItems(saleItems);

        saleRepo.save(sale);
        invRepo.saveAll(updatedInventories);
        salesAnalyticsRepo.saveAll(analyticsLogs);

        for (RetailerInventory invItem : updatedInventories) {
            lowStockScheduler.checkSingleItemLowStock(invItem);
        }

        return ResponseEntity.ok(ApiResponse.ok("Sale transaction successful", toSaleResponse(sale)));
    }

    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<PageResponse<SaleResponse>>> getSales(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = getUser(auth);
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("saleDate").descending());
        org.springframework.data.domain.Page<RetailerSale> salePage = saleRepo.findByRetailerId(user.getId(), pageable);
        
        List<SaleResponse> list = salePage.getContent().stream()
                .map(this::toSaleResponse)
                .collect(Collectors.toList());
                
        PageResponse<SaleResponse> pageResponse = PageResponse.<SaleResponse>builder()
                .content(list)
                .page(salePage.getNumber())
                .size(salePage.getSize())
                .totalElements(salePage.getTotalElements())
                .totalPages(salePage.getTotalPages())
                .last(salePage.isLast())
                .build();
                
        return ResponseEntity.ok(ApiResponse.ok("Sales list fetched successfully", pageResponse));
    }

    @GetMapping("/sales/today-summary")
    public ResponseEntity<ApiResponse<TodaySalesSummary>> getTodaySalesSummary(Authentication auth) {
        User user = getUser(auth);
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(java.time.LocalTime.MAX);
        
        BigDecimal totalAmount = saleRepo.sumTotalAmountByRetailerIdAndSaleDateBetween(user.getId(), start, end);
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
        long totalCount = saleRepo.countByRetailerIdAndSaleDateBetween(user.getId(), start, end);
        
        TodaySalesSummary summary = TodaySalesSummary.builder()
                .totalAmount(totalAmount)
                .totalCount(totalCount)
                .build();
        return ResponseEntity.ok(ApiResponse.ok("Today's sales summary fetched successfully", summary));
    }

    @PostMapping("/orders")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            Authentication auth, @RequestBody OrderRequest req) {
        User retailer = getUser(auth);
        User supplier = userRepo.findById(req.getSupplierId())
                .orElseThrow(() -> new BadRequestException("Supplier not found"));
        Product product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new BadRequestException("Product not found"));

        Order order = Order.builder()
                .retailer(retailer)
                .supplier(supplier)
                .orderDate(LocalDateTime.now())
                .status("PENDING")
                .build();
        orderRepo.save(order);

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(req.getQuantity())
                .price(req.getPrice())
                .unit(req.getUnit())
                .build();
        orderItemRepo.save(item);

        return ResponseEntity.ok(ApiResponse.ok("Order created successfully", toOrderResponse(order)));
    }

    private List<String> getUnexpiredPlanNames(User user) {
        return subRepo.findByUserId(user.getId()).stream()
                .filter(s -> s.getEndDateTime() != null && s.getEndDateTime().isAfter(LocalDateTime.now()))
                .map(s -> s.getPlanName().name())
                .distinct()
                .collect(Collectors.toList());
    }

    @GetMapping("/subscription")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscription(Authentication auth) {
        User user = getUser(auth);
        List<String> unexpired = getUnexpiredPlanNames(user);
        List<Subscription> userSubs = subRepo.findByUserId(user.getId());
        
        java.util.Optional<Subscription> currentSubOpt = userSubs.stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .findFirst();
                
        if (!currentSubOpt.isPresent()) {
            currentSubOpt = userSubs.stream()
                    .filter(s -> s.getStartDateTime() != null)
                    .max(java.util.Comparator.comparing(Subscription::getStartDateTime));
        }

        return currentSubOpt
            .map(s -> ResponseEntity.ok(ApiResponse.ok("Subscription", SubscriptionResponse.builder()
                .id(s.getId()).planName(s.getPlanName().name()).status(s.getStatus())
                .startDateTime(s.getStartDateTime()).endDateTime(s.getEndDateTime())
                .unexpiredPlans(unexpired).build())))
            .orElse(ResponseEntity.ok(ApiResponse.ok("No subscription", SubscriptionResponse.builder()
                .unexpiredPlans(unexpired).build())));
    }

    @PostMapping("/subscription/upgrade")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<SubscriptionResponse>> upgradeSubscription(
            Authentication auth, @RequestParam("plan") String planName) {
        User user = getUser(auth);
        
        List<Subscription> userSubs = subRepo.findByUserId(user.getId());
        
        java.util.Optional<Subscription> activeSub = userSubs.stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()) && s.getEndDateTime().isAfter(LocalDateTime.now()))
                .findFirst();
        if (activeSub.isPresent() && activeSub.get().getPlanName().name().equalsIgnoreCase(planName)) {
            throw new BadRequestException("You already have an active subscription for the " + planName + " plan.");
        }
        
        // Find existing unexpired subscription for this plan
        java.util.Optional<Subscription> unexpiredSub = userSubs.stream()
                .filter(s -> s.getPlanName().name().equalsIgnoreCase(planName) && s.getEndDateTime().isAfter(LocalDateTime.now()))
                .findFirst();

        Subscription sub;
        
        // Mark all currently active ones to EXPIRED
        List<Subscription> activeSubs = userSubs.stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .collect(Collectors.toList());
        for (Subscription s : activeSubs) {
            s.setStatus("EXPIRED");
            subRepo.save(s);
        }

        if (unexpiredSub.isPresent()) {
            // Reactivate existing unexpired plan without payment
            sub = unexpiredSub.get();
            sub.setStatus("ACTIVE");
            subRepo.save(sub);
        } else {
            // Normal charge and new subscription
            BigDecimal amount = BigDecimal.ZERO;
            if ("BASIC".equalsIgnoreCase(planName)) amount = BigDecimal.valueOf(499);
            else if ("PREMIUM".equalsIgnoreCase(planName)) amount = BigDecimal.valueOf(1499);
            
            Payment payment = Payment.builder()
                    .user(user)
                    .amount(amount)
                    .paymentGateway("SIMULATION")
                    .transactionId("TXN-" + System.currentTimeMillis())
                    .status("SUCCESS")
                    .build();
            paymentRepo.save(payment);

            sub = Subscription.builder()
                    .user(user)
                    .payment(payment)
                    .planName(SubscriptionPlan.valueOf(planName.toUpperCase()))
                    .status("ACTIVE")
                    .startDateTime(LocalDateTime.now())
                    .endDateTime(LocalDateTime.now().plusMonths(1))
                    .build();
            subRepo.save(sub);
        }

        List<String> unexpired = getUnexpiredPlanNames(user);
        SubscriptionResponse resp = SubscriptionResponse.builder()
                .id(sub.getId())
                .planName(sub.getPlanName().name())
                .status(sub.getStatus())
                .startDateTime(sub.getStartDateTime())
                .endDateTime(sub.getEndDateTime())
                .unexpiredPlans(unexpired)
                .build();
        return ResponseEntity.ok(ApiResponse.ok("Subscription upgraded successfully", resp));
    }

    private SaleResponse toSaleResponse(RetailerSale sale) {
        List<SaleResponse.SaleItemResponse> items = sale.getItems().stream()
                .map(i -> SaleResponse.SaleItemResponse.builder()
                        .id(i.getId())
                        .productId(i.getProduct() != null ? i.getProduct().getId() : null)
                        .productName(i.getProduct() != null ? i.getProduct().getName() : "—")
                        .quantity(i.getQuantity())
                        .price(i.getPrice())
                        .unit(i.getUnit())
                        .build())
                .collect(Collectors.toList());

        return SaleResponse.builder()
                .id(sale.getId())
                .saleDate(sale.getSaleDate())
                .totalAmount(sale.getTotalAmount())
                .retailerEmail(sale.getRetailer() != null ? sale.getRetailer().getEmail() : null)
                .items(items)
                .build();
    }
}
