package com.smartinventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "supplier_inventories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierInventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "userid")
    private User user;

    private BigDecimal price;
    private Integer moq;
    private Integer stockQuantity;
    private LocalDate availability;
    private Integer leadTime;
    private Boolean isActive = true;

    @OneToMany(mappedBy = "supplierInventory")
    private List<SupplierInventoryImage> images;
}
