package com.smartinventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "supplier_inventory_images")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierInventoryImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "supplierinventoryid")
    private SupplierInventory supplierInventory;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;
}
