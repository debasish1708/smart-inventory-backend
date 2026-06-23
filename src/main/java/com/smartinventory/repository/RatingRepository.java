package com.smartinventory.repository;

import com.smartinventory.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findBySupplierId(Long supplierId);
    List<Rating> findByRetailerId(Long retailerId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Rating r WHERE r.supplier.id = :supplierId")
    Double avgRatingForSupplier(Long supplierId);
}
