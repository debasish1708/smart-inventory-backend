package com.smartinventory.entity;

import com.smartinventory.enums.ProfileStatus;
import com.smartinventory.enums.Role;
import com.smartinventory.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    private ProfileStatus profileStatus;

    private LocalDateTime emailVerifiedAt;
    private LocalDateTime adminVerifiedAt;
    private LocalDateTime lastLoginAt;
}
