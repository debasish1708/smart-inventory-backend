package com.smartinventory.repository;

import com.smartinventory.entity.RetailerProductRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetailerProductRecommendationRepository extends JpaRepository<RetailerProductRecommendation, Long> {
}
