package com.smartinventory.repository;

import com.smartinventory.entity.SupplierInventoryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierInventoryImageRepository extends JpaRepository<SupplierInventoryImage, Long> {
}
