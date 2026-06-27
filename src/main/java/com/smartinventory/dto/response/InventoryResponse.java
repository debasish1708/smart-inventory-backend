package com.smartinventory.dto.response;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryResponse {
    private Long      id;
    private Long      productId;
    private String    productName;
    private String    category;
    private String    brand;
    private String    unit;
    private String    sku;
    private BigDecimal price;
    // retailer fields
    private Integer   quantity;
    private Integer   thresholdValue;
    // supplier fields
    private Integer   moq;
    private Integer   stockQuantity;
    private Integer   leadTime;
    private Boolean   isActive;
}
