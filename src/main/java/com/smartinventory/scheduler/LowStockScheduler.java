package com.smartinventory.scheduler;

import com.smartinventory.entity.NotificationRetailer;
import com.smartinventory.entity.RetailerInventory;
import com.smartinventory.enums.NotificationType;
import com.smartinventory.repository.NotificationRepository;
import com.smartinventory.repository.RetailerInventoryRepository;
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

    @Scheduled(fixedRate = 3_600_000) // every 1 hour
    @Transactional
    public void checkLowStock() {
        List<RetailerInventory> lowItems = retailerInventoryRepository.findAllLowStockItems();
        if (lowItems.isEmpty()) { log.debug("Low-stock check: no items below threshold"); return; }

        log.info("Low-stock check: {} items found below threshold", lowItems.size());

        for (RetailerInventory item : lowItems) {
            String message = String.format(
                "Low stock alert: '%s' has only %d units remaining (threshold: %d). Visit supplier match to reorder.",
                item.getProduct().getName(), item.getQuantity(), item.getThresholdValue());

            NotificationRetailer notification = NotificationRetailer.builder()
                .retailer(item.getUser())
                .product(item.getProduct())
                .type(NotificationType.LOW_STOCK)
                .message(message)
                .isRead(false)
                .notificationSentAt(LocalDateTime.now())
                .build();

            notificationRepository.save(notification);
        }
    }
}
