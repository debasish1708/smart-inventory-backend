package com.smartinventory.repository;

import com.smartinventory.entity.RetailerSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RetailerSaleItemRepository extends JpaRepository<RetailerSaleItem, Long> {
    List<RetailerSaleItem> findByRetailerSaleId(Long retailerSaleId);
}
