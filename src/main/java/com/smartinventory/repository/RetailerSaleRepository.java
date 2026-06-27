package com.smartinventory.repository;

import com.smartinventory.entity.RetailerSale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RetailerSaleRepository extends JpaRepository<RetailerSale, Long> {
    List<RetailerSale> findByRetailerIdOrderBySaleDateDesc(Long retailerId);
    
    Page<RetailerSale> findByRetailerId(Long retailerId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM RetailerSale s WHERE s.retailer.id = :retailerId AND s.saleDate >= :start AND s.saleDate <= :end")
    BigDecimal sumTotalAmountByRetailerIdAndSaleDateBetween(
            @Param("retailerId") Long retailerId, 
            @Param("start") LocalDateTime start, 
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(s) FROM RetailerSale s WHERE s.retailer.id = :retailerId AND s.saleDate >= :start AND s.saleDate <= :end")
    long countByRetailerIdAndSaleDateBetween(
            @Param("retailerId") Long retailerId, 
            @Param("start") LocalDateTime start, 
            @Param("end") LocalDateTime end);
}

