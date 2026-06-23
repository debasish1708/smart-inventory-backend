package com.smartinventory.repository;

import com.smartinventory.entity.RetailerInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RetailerInventoryRepository extends JpaRepository<RetailerInventory, Long> {

    @Query("SELECT r FROM RetailerInventory r WHERE r.quantity <= r.thresholdValue")
    List<RetailerInventory> findAllLowStockItems();
}
