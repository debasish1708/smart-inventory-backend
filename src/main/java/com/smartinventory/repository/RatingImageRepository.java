package com.smartinventory.repository;

import com.smartinventory.entity.RatingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RatingImageRepository extends JpaRepository<RatingImage, Long> {
}
