package com.smartinventory.dto.response;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RatingResponse {
    private Long          id;
    private Integer       rating;
    private String        review;
    private String        supplierEmail;
    private String        retailerEmail;
    private LocalDateTime createdAt;
    private java.util.List<String> images;
}
