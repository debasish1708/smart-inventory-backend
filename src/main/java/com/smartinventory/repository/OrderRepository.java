package com.smartinventory.repository;

import com.smartinventory.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByRetailerIdOrderByOrderDateDesc(Long retailerId);
    List<Order> findBySupplierIdOrderByOrderDateDesc(Long supplierId);
    long countByRetailerId(Long retailerId);
    long countBySupplierId(Long supplierId);
    long countByRetailerIdAndStatus(Long retailerId, String status);
    long countBySupplierIdAndStatus(Long supplierId, String status);

    @Query("SELECT COALESCE(SUM(oi.price * oi.quantity), 0) FROM OrderItem oi WHERE oi.order.supplier.id = :supplierId AND oi.order.status = 'DELIVERED'")
    java.math.BigDecimal sumRevenueForSupplier(Long supplierId);

    @Query("SELECT COALESCE(SUM(oi.price * oi.quantity), 0) FROM OrderItem oi WHERE oi.order.retailer.id = :retailerId AND oi.order.status = 'DELIVERED'")
    java.math.BigDecimal sumRevenueForRetailer(Long retailerId);
}
