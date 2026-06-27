package com.smartinventory.entity;

import com.smartinventory.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "notification_retailer")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationRetailer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "retailerid", nullable = false)
    private User retailer;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String message;

    @Builder.Default
    private Boolean isRead = false;

    private LocalDateTime notificationSentAt;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RetailerProductRecommendation> recommendations;
}
