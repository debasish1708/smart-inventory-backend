package com.smartinventory.repository;

import com.smartinventory.entity.RetailerSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RetailerSaleRepository extends JpaRepository<RetailerSale, Long> {
    List<RetailerSale> findByRetailerIdOrderBySaleDateDesc(Long retailerId);
}
