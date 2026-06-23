package com.smartinventory.dto.request;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class InventoryRequest {
    private String  productName;
    private String  category;
    private String  brand;
    private String  unit;
    private String  sku;
    private BigDecimal price;
    private Integer quantity;         // retailer
    private Integer thresholdValue;   // retailer
    private Integer moq;              // supplier
    private Integer stockQuantity;    // supplier
    private Integer leadTime;         // supplier
    private Boolean isActive;         // supplier
}
