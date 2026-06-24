package com.smartinventory.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductSummaryResponse {

    private Long   id;
    private String name;
    private String description;
    private String sku;
    private String brand;
    private String unit;
    private Long   categoryId;
    private String categoryName;
}
