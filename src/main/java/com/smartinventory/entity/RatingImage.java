package com.smartinventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rating_images")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RatingImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ratingid", nullable = false)
    private Rating rating;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;
}
