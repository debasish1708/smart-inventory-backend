package com.smartinventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

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

    @OneToOne
    @JoinColumn(name = "orderid")
    private Order order;

    @OneToMany(mappedBy = "rating", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RatingImage> images;
}
