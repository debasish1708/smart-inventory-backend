package com.smartinventory.entity;

import com.smartinventory.enums.RecommendationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "retailer_product_recommendations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RetailerProductRecommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notificationid", nullable = false)
    private NotificationRetailer notification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplierid", nullable = false)
    private User supplier;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private Integer rank;

    @Enumerated(EnumType.STRING)
    private RecommendationStatus status;
}
