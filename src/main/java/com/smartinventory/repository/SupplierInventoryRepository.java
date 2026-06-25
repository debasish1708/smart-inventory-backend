package com.smartinventory.repository;

import com.smartinventory.entity.SupplierInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SupplierInventoryRepository extends JpaRepository<SupplierInventory, Long> {
    List<SupplierInventory> findByUserIdAndIsActiveTrue(Long userId);
    List<SupplierInventory> findByUserId(Long userId);

    @Query("SELECT si FROM SupplierInventory si JOIN si.product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%',:name,'%')) AND si.isActive = true AND si.stockQuantity > 0")
    List<SupplierInventory> searchByProductName(String name);

    @Query("SELECT si FROM SupplierInventory si WHERE si.product.id = :productId AND si.isActive = true AND si.stockQuantity > 0")
    List<SupplierInventory> findByProductId(@Param("productId") Long productId);

    @Query("SELECT si FROM SupplierInventory si WHERE si.isActive = true AND si.stockQuantity > 0")
    List<SupplierInventory> findAllActive();
}
