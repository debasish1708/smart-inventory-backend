package com.smartinventory.dto.response;
import lombok.*;
@Data @Builder
public class ProfileResponse {
    private Long    userId;
    private String  email;
    private String  role;
    private String  userStatus;
    private boolean profileCompleted;
    private String firstName;
    private String lastName;
    private String businessName;
    private String businessType;
    private String mobNo;
    private String address;
    private String gst;
    private String city;
    private String state;
    private String pincode;
    private String profileImageUrl;
}
