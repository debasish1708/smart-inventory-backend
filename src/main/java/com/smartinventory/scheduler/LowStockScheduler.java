package com.smartinventory.scheduler;

import com.smartinventory.entity.NotificationRetailer;
import com.smartinventory.entity.RetailerInventory;
import com.smartinventory.entity.RetailerProductRecommendation;
import com.smartinventory.entity.SupplierInventory;
import com.smartinventory.enums.NotificationType;
import com.smartinventory.enums.RecommendationStatus;
import com.smartinventory.repository.NotificationRepository;
import com.smartinventory.repository.RetailerInventoryRepository;
import com.smartinventory.repository.SupplierInventoryRepository;
import com.smartinventory.repository.RatingRepository;
import com.smartinventory.repository.RetailerProductRecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LowStockScheduler {

    private final RetailerInventoryRepository retailerInventoryRepository;
    private final NotificationRepository      notificationRepository;
    private final SupplierInventoryRepository supplierInventoryRepository;
    private final RatingRepository            ratingRepository;
    private final RetailerProductRecommendationRepository recommendationRepository;

    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void checkLowStock() {
        List<RetailerInventory> lowItems = retailerInventoryRepository.findAllLowStockItems();
        if (lowItems.isEmpty()) { 
            log.debug("Low-stock check: no items below threshold"); 
            return; 
        }

        log.info("Low-stock check: {} items found below threshold", lowItems.size());

        for (RetailerInventory item : lowItems) {
            checkAndNotify(item);
        }
    }

    @Transactional
    public void checkSingleItemLowStock(RetailerInventory item) {
        if (item.getQuantity() <= item.getThresholdValue()) {
            checkAndNotify(item);
        }
    }

    private void checkAndNotify(RetailerInventory item) {
        if (item.getUser() == null || item.getProduct() == null) {
            return;
        }

        // Prevent duplicate unread notifications for same product and retailer
        if (notificationRepository.existsByRetailerIdAndProductIdAndTypeAndIsReadFalse(
                item.getUser().getId(), item.getProduct().getId(), NotificationType.LOW_STOCK)) {
            log.debug("Unread low-stock notification already exists for retailer {} and product {}", 
                      item.getUser().getId(), item.getProduct().getId());
            return;
        }

        // Get active supplier inventories for this product
        List<SupplierInventory> suppliers = supplierInventoryRepository.findByProductId(item.getProduct().getId());

        // Sort suppliers based on scoring logic
        sortSuppliers(suppliers);

        String message;
        if (!suppliers.isEmpty()) {
            SupplierInventory best = suppliers.get(0);
            Double avgRating = ratingRepository.avgRatingForSupplier(best.getUser().getId());
            message = String.format(
                "Low stock alert: '%s' has only %d units remaining (threshold: %d). Best option: Supplier '%s' offers at $%s (MOQ: %d, Rating: %.1f).",
                item.getProduct().getName(), item.getQuantity(), item.getThresholdValue(),
                best.getUser().getEmail(), best.getPrice().toString(), best.getMoq(), avgRating != null ? avgRating : 0.0
            );
        } else {
            message = String.format(
                "Low stock alert: '%s' has only %d units remaining (threshold: %d). No active suppliers available currently.",
                item.getProduct().getName(), item.getQuantity(), item.getThresholdValue()
            );
        }

        NotificationRetailer notification = NotificationRetailer.builder()
            .retailer(item.getUser())
            .product(item.getProduct())
            .type(NotificationType.LOW_STOCK)
            .message(message)
            .isRead(false)
            .notificationSentAt(LocalDateTime.now())
            .build();

        NotificationRetailer saved = notificationRepository.save(notification);

        // Save recommendations for top 5 suppliers
        for (int i = 0; i < Math.min(suppliers.size(), 5); i++) {
            SupplierInventory si = suppliers.get(i);
            RetailerProductRecommendation rec = RetailerProductRecommendation.builder()
                .notification(saved)
                .supplier(si.getUser())
                .price(si.getPrice())
                .rank(i + 1)
                .status(RecommendationStatus.PENDING)
                .build();
            recommendationRepository.save(rec);
        }
    }

    private void sortSuppliers(List<SupplierInventory> suppliers) {
        if (suppliers.size() <= 1) return;

        double minCost = Double.MAX_VALUE;
        double maxCost = 0.0;
        double minLead = Double.MAX_VALUE;
        double maxLead = 0.0;
        double minRating = Double.MAX_VALUE;
        double maxRating = 0.0;

        for (SupplierInventory si : suppliers) {
            double cost = si.getPrice().doubleValue() * si.getMoq();
            double lead = si.getLeadTime() != null ? si.getLeadTime().doubleValue() : 1.0;
            Double r = ratingRepository.avgRatingForSupplier(si.getUser().getId());
            double rating = r != null ? r : 0.0;

            if (cost < minCost) minCost = cost;
            if (cost > maxCost) maxCost = cost;
            if (lead < minLead) minLead = lead;
            if (lead > maxLead) maxLead = lead;
            if (rating < minRating) minRating = rating;
            if (rating > maxRating) maxRating = rating;
        }

        final double finalMinCost = minCost;
        final double finalMaxCost = maxCost;
        final double finalMinLead = minLead;
        final double finalMaxLead = maxLead;
        final double finalMinRating = minRating;
        final double finalMaxRating = maxRating;

        suppliers.sort((s1, s2) -> {
            double cost1 = s1.getPrice().doubleValue() * s1.getMoq();
            double lead1 = s1.getLeadTime() != null ? s1.getLeadTime().doubleValue() : 1.0;
            Double r1 = ratingRepository.avgRatingForSupplier(s1.getUser().getId());
            double rating1 = r1 != null ? r1 : 0.0;

            double cost2 = s2.getPrice().doubleValue() * s2.getMoq();
            double lead2 = s2.getLeadTime() != null ? s2.getLeadTime().doubleValue() : 1.0;
            Double r2 = ratingRepository.avgRatingForSupplier(s2.getUser().getId());
            double rating2 = r2 != null ? r2 : 0.0;

            // 1. Cost factor (lower is better -> 1 - normalizedCost)
            double costRange = finalMaxCost - finalMinCost;
            double costFactor1 = (costRange == 0) ? 1.0 : 1.0 - ((cost1 - finalMinCost) / costRange);
            double costFactor2 = (costRange == 0) ? 1.0 : 1.0 - ((cost2 - finalMinCost) / costRange);

            // 2. Rating factor (higher is better)
            double ratingRange = finalMaxRating - finalMinRating;
            double ratingFactor1 = (ratingRange == 0) ? 1.0 : ((rating1 - finalMinRating) / ratingRange);
            double ratingFactor2 = (ratingRange == 0) ? 1.0 : ((rating2 - finalMinRating) / ratingRange);

            // 3. Lead time factor (lower is better)
            double leadRange = finalMaxLead - finalMinLead;
            double leadFactor1 = (leadRange == 0) ? 1.0 : 1.0 - ((lead1 - finalMinLead) / leadRange);
            double leadFactor2 = (leadRange == 0) ? 1.0 : 1.0 - ((lead2 - finalMinLead) / leadRange);

            // Composite score: 40% Cost, 30% Rating, 30% Availability (Lead Time)
            double score1 = costFactor1 * 0.4 + ratingFactor1 * 0.3 + leadFactor1 * 0.3;
            double score2 = costFactor2 * 0.4 + ratingFactor2 * 0.3 + leadFactor2 * 0.3;

            return Double.compare(score2, score1);
        });
    }
}
