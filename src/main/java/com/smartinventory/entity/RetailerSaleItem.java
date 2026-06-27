package com.smartinventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "retailer_sale_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RetailerSaleItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailersaleid", nullable = false)
    private RetailerSale retailerSale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productid", nullable = false)
    private Product product;

    private Integer quantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private String unit;
}
