package com.smartinventory.repository;

import com.smartinventory.entity.NotificationRetailer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationRetailer, Long> {
    List<NotificationRetailer> findByRetailerIdOrderByCreatedAtDesc(Long retailerId);
    long countByRetailerIdAndIsReadFalse(Long retailerId);
}
