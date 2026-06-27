package com.smartinventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "retailer_sales")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RetailerSale extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailerid", nullable = false)
    private User retailer;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;

    private LocalDateTime saleDate;

    @OneToMany(mappedBy = "retailerSale", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RetailerSaleItem> items;
}
