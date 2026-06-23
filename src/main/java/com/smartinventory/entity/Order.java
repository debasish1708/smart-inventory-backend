package com.smartinventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "supplierid")
    private User supplier;

    @ManyToOne
    @JoinColumn(name = "retailerid")
    private User retailer;

    private LocalDateTime orderDate;
    private LocalDateTime deliveredDate;
    private String status;
}
