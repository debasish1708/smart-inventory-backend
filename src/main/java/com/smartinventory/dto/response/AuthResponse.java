package com.smartinventory.dto.response;
import lombok.*;
@Data @Builder
public class AuthResponse {
    private String  token;
    private String  email;
    private String  role;
    private String  status;
    private Long    userId;
    private boolean profileCompleted;
}
