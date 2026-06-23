package com.smartinventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ratings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Rating extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "supplierid")
    private User supplier;

    @ManyToOne
    @JoinColumn(name = "retailerid")
    private User retailer;

    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String review;
}
