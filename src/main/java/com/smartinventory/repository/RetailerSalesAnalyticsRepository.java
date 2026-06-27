package com.smartinventory.repository;

import com.smartinventory.entity.RetailerSalesAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetailerSalesAnalyticsRepository extends JpaRepository<RetailerSalesAnalytics, Long> {
}
