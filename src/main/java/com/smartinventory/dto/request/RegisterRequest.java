package com.smartinventory.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data
public class RegisterRequest {
    @NotBlank @Email
    private String email;
    @NotBlank @Size(min = 6)
    private String password;
    @NotBlank @Pattern(regexp = "RETAILER|SUPPLIER", flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "role must be RETAILER or SUPPLIER")
    private String role;
}
