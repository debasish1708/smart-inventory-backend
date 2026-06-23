package com.smartinventory.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data
public class UpdateProfileRequest {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @NotBlank private String businessName;
    private String businessType;
    @NotBlank private String mobNo;
    @NotBlank private String address;
    private String gst;
    @NotBlank private String city;
    @NotBlank private String state;
    @NotBlank private String pincode;
    private String profileImageUrl;
}
