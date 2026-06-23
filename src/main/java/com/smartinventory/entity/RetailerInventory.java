package com.smartinventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "retailer_inventories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RetailerInventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "userid")
    private User user;

    private Integer thresholdValue = 10;
    private BigDecimal price;
    private Integer quantity;
}
