package com.smartinventory.dto.response;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SupplierMatchResponse {
    private Long       productId;
    private Long       supplierId;
    private String     supplierName;
    private String     businessName;
    private String     productName;
    private String     category;
    private BigDecimal price;
    private Integer    moq;
    private Integer    stockQuantity;
    private Integer    leadTime;
    private Double     rating;
}
