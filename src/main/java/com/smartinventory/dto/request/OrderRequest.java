package com.smartinventory.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderRequest {
    private Long supplierId;
    private Long productId;
    private Integer quantity;
    private BigDecimal price;
    private String unit;
}
