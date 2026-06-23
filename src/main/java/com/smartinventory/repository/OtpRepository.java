package com.smartinventory.repository;

import com.smartinventory.entity.Otp;
import com.smartinventory.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findTopByUserIdAndTypeAndIsUsedFalseOrderByCreatedAtDesc(Long userId, OtpType type);
}
