package com.smartinventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Profile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "userid", nullable = false, unique = true)
    private User user;

    private String firstName;
    private String lastName;
    private String businessName;
    private String businessType;
    private String mobNo;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String gst;
    private String city;
    private String state;
    private String pincode;
    private String profileImageUrl;
}
